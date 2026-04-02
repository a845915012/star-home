package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.starhome.domain.FurnitureNumberApiPoolDO;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageItemResp;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageResp;
import com.ruoyi.starhome.mapper.FurnitureNumberApiPoolMapper;
import com.ruoyi.starhome.mapper.FurnitureVideoTaskMapper;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FurnitureVideoTaskServiceImpl implements IFurnitureVideoTaskService {

    private static final String IMAGE2IMAGE_T8STAR_API = "image2image_t8star_api";
    private static final String VIDEO_GENERATION_STATUS_URL = "https://ai.t8star.cn/v2/videos/generations/";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Autowired
    private FurnitureVideoTaskMapper furnitureVideoTaskMapper;

    @Autowired
    private FurnitureNumberApiPoolMapper furnitureNumberApiPoolMapper;

    @Override
    public FurnitureVideoTaskPageResp selectPage(FurnitureVideoTaskPageRequest request) {
        Long userId = SecurityUtils.getUserId();

        try (com.github.pagehelper.Page<Object> page = PageHelper.startPage(request.getPageNum(), request.getPageSize())) {
            List<FurnitureVideoTaskDO> records = furnitureVideoTaskMapper.selectList(
                    new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                            .eq(FurnitureVideoTaskDO::getUserId, userId)
                            .like(request.getPrompt() != null && !request.getPrompt().trim().isEmpty(),
                                    FurnitureVideoTaskDO::getPrompt, request.getPrompt().trim())
                            .orderByDesc(FurnitureVideoTaskDO::getId)
            );

            FurnitureVideoTaskPageResp resp = new FurnitureVideoTaskPageResp();
            resp.setTotal(page.getTotal());
            resp.setList(convertList(records));
            return resp;
        }
    }

    @Override
    public String getProcessByTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new ServiceException("taskId不能为空");
        }

        FurnitureNumberApiPoolDO apiPool = furnitureNumberApiPoolMapper.selectOne(
                new LambdaQueryWrapper<FurnitureNumberApiPoolDO>()
                        .eq(FurnitureNumberApiPoolDO::getNumber, IMAGE2IMAGE_T8STAR_API)
                        .last("limit 1")
        );
        if (apiPool == null || apiPool.getApiKey() == null || apiPool.getApiKey().isBlank()) {
            throw new ServiceException("未找到image2image_t8star_api的token配置");
        }

        String responseText = queryVideoTaskProcess(taskId, apiPool.getApiKey());
        updateVideoTaskByResponse(taskId, responseText);
        return responseText;
    }

    private String queryVideoTaskProcess(String taskId, String token) {
        Request request = new Request.Builder()
                .url(VIDEO_GENERATION_STATUS_URL + taskId)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new ServiceException("查询视频任务进度失败: " + response.code() + " - " + responseText);
            }
            if (responseText == null || responseText.isBlank()) {
                throw new ServiceException("查询视频任务进度失败: 响应体为空");
            }
            return responseText;
        } catch (IOException e) {
            throw new ServiceException("查询视频任务进度异常: " + e.getMessage());
        }
    }

    private void updateVideoTaskByResponse(String taskId, String responseText) {
        try {
            JsonNode root = objectMapper.readTree(responseText);
            JsonNode dataNode = root.path("data");

            FurnitureVideoTaskDO update = new FurnitureVideoTaskDO();
            update.setTaskId(taskId);
            update.setStatus(getText(root, "status", getText(dataNode, "status", null)));
            update.setProgress(getProgress(root, dataNode));
            update.setFailReason(getText(root, "fail_reason", getText(dataNode, "error", null)));
            update.setModel(getText(dataNode, "model", null));
            update.setCost(getBigDecimal(root, "cost"));
            update.setSize(getText(dataNode, "size", null));
            update.setSeconds(getInteger(dataNode, "seconds"));
            update.setVideoUrlRemote(getText(dataNode, "video_url", null));

            Long finishTimeSeconds = getLong(root, "finish_time");
            if (finishTimeSeconds == null || finishTimeSeconds <= 0) {
                finishTimeSeconds = getLong(dataNode, "completed_at");
            }
            if (finishTimeSeconds != null && finishTimeSeconds > 0) {
                update.setFinishTime(new Date(finishTimeSeconds * 1000));
            }

            update.setIsComplete(isCompletedStatus(update.getStatus()) ? 1 : 0);

            furnitureVideoTaskMapper.update(
                    update,
                    new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                            .eq(FurnitureVideoTaskDO::getTaskId, taskId)
            );
        } catch (Exception e) {
            log.error("更新视频任务进度失败, taskId={}, resp={}", taskId, responseText, e);
            throw new ServiceException("更新视频任务进度失败: " + e.getMessage());
        }
    }

    private boolean isCompletedStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return "SUCCESS".equals(normalized) || "SUCCEEDED".equals(normalized) || "COMPLETED".equals(normalized) || "FAILED".equals(normalized);
    }

    private String getProgress(JsonNode root, JsonNode dataNode) {
        String progress = getText(root, "progress", null);
        if (progress != null && !progress.isBlank()) {
            return progress;
        }

        JsonNode dataProgressNode = dataNode.path("progress");
        if (!dataProgressNode.isMissingNode() && !dataProgressNode.isNull()) {
            if (dataProgressNode.isNumber()) {
                return dataProgressNode.asInt() + "%";
            }
            String dataProgress = dataProgressNode.asText();
            if (dataProgress != null && !dataProgress.isBlank()) {
                return dataProgress;
            }
        }
        return null;
    }

    private String getText(JsonNode node, String field, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return defaultValue;
        }
        String text = valueNode.asText();
        return text == null || text.isBlank() ? defaultValue : text;
    }

    private Integer getInteger(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isInt() || valueNode.isLong()) {
            return valueNode.asInt();
        }
        String text = valueNode.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getLong(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isLong() || valueNode.isInt()) {
            return valueNode.asLong();
        }
        String text = valueNode.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getBigDecimal(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(valueNode.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private List<FurnitureVideoTaskPageItemResp> convertList(List<FurnitureVideoTaskDO> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::convertItem).collect(Collectors.toList());
    }

    private FurnitureVideoTaskPageItemResp convertItem(FurnitureVideoTaskDO item) {
        FurnitureVideoTaskPageItemResp resp = new FurnitureVideoTaskPageItemResp();
        BeanUtils.copyProperties(item, resp);
        return resp;
    }
}
