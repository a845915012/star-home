package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.starhome.domain.FurnitureConsumeConfigDO;
import com.ruoyi.starhome.domain.FurnitureVideoGenerationTaskDO;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageItemResp;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageResp;
import com.ruoyi.starhome.mapper.FurnitureVideoGenerationTaskMapper;
import com.ruoyi.starhome.mapper.FurnitureVideoTaskMapper;
import com.ruoyi.starhome.service.IFurnitureConsumeConfigService;
import com.ruoyi.starhome.service.IFurnitureUserBalanceAccountService;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import com.ruoyi.starhome.util.StarhomeFileUrlUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.Response;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FurnitureVideoTaskServiceImpl implements IFurnitureVideoTaskService {

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${starhome.vimax-agent.base-url}")
    private String vimaxAgentBaseUrl;

    @Value("${starhome.vimax-agent.api-key:}")
    private String vimaxAgentApiKey;

    @Autowired
    private FurnitureVideoTaskMapper furnitureVideoTaskMapper;

    @Autowired
    private FurnitureVideoGenerationTaskMapper furnitureVideoGenerationTaskMapper;

    @Autowired
    private IFurnitureUserBalanceAccountService furnitureUserBalanceAccountService;

    @Autowired
    private IFurnitureConsumeConfigService furnitureConsumeConfigService;

    @Autowired
    private ITaskApiInvokeService taskApiInvokeService;

    @Autowired
    private StarhomeFileUrlUtils starhomeFileUrlUtils;


    private BigDecimal resolveVideoConsumePrice(FurnitureVideoGenerationTaskDO header) {
        if (header != null && header.getConsumePrice() != null) {
            return header.getConsumePrice();
        }
        String consumeCode = header == null || header.getConsumeCode() == null || header.getConsumeCode().isBlank()
                ? "IMAGE2VIDEO" : header.getConsumeCode();
        FurnitureConsumeConfigDO consumeConfig = furnitureConsumeConfigService.selectEnabledByCode(consumeCode);
        return consumeConfig.getPrice() == null ? BigDecimal.ZERO : consumeConfig.getPrice();
    }

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
    public List<FurnitureVideoTaskDO> listByGenerationTaskId(Long generationTaskId) {
        if (generationTaskId == null) {
            throw new ServiceException("generationTaskId不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        return furnitureVideoTaskMapper.selectList(
                new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                        .eq(FurnitureVideoTaskDO::getGenerationTaskId, generationTaskId)
                        .eq(FurnitureVideoTaskDO::getUserId, userId)
                        .orderByAsc(FurnitureVideoTaskDO::getStartTime)
                        .orderByAsc(FurnitureVideoTaskDO::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String getProcessByTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new ServiceException("taskId不能为空");
        }
        if (vimaxAgentBaseUrl == null || vimaxAgentBaseUrl.isBlank()) {
            throw new ServiceException("vimax-agent base-url 未配置（starhome.vimax-agent.base-url）");
        }

        FurnitureVideoTaskDO task = furnitureVideoTaskMapper.selectOne(
                new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                        .eq(FurnitureVideoTaskDO::getTaskId, taskId)
                        .last("limit 1")
        );
        if (task == null) {
            throw new ServiceException("未找到视频任务: " + taskId);
        }

        String responseText = queryVimaxJobStatus(taskId);
        try {
            JsonNode root = objectMapper.readTree(responseText);
            String vimaxStatus = getText(root, "status");
            String mappedStatus = mapVimaxStatusToLocal(vimaxStatus);
            String progress = getText(root, "progress");
            String error = getText(root, "error");
            String resultUrl = getText(root, "result_url");
            String downloadUrl = buildDownloadUrl(taskId, resultUrl);

            Date finishedAt = parseIsoDate(getText(root, "finished_at"));

            FurnitureVideoTaskDO update = new FurnitureVideoTaskDO();
            update.setId(task.getId());
            update.setStatus(mappedStatus);
            update.setProgress(progress);
            update.setFailReason(error);
            update.setVideoUrlRemote(downloadUrl);
            if (finishedAt != null) {
                update.setFinishTime(finishedAt);
            }

            if ("failed".equalsIgnoreCase(vimaxStatus)) {
                update.setIsComplete(1);
                furnitureVideoTaskMapper.updateById(update);
                markHeaderFailedIfNeeded(task.getGenerationTaskId(), error == null || error.isBlank() ? "任务失败" : error);
                taskApiInvokeService.completeDeferredVideoUsageRecord(task.getGenerationTaskId(), null, "FAIL");
                return responseText;
            }

            if ("completed".equalsIgnoreCase(vimaxStatus)) {
                String localUrl = task.getVideoUrlLocal();
                if (localUrl == null || localUrl.isBlank()) {
                    localUrl = downloadVimaxVideoToProfile(taskId);
                    update.setVideoUrlLocal(localUrl);
                }

                String remoteUrl = finalizeHeaderIfNeeded(task, localUrl);
                update.setVideoUrlRemote(remoteUrl);
                update.setIsComplete(1);
                update.setStatus("success");
                furnitureVideoTaskMapper.updateById(update);
                return responseText;
            }

            update.setIsComplete(0);
            furnitureVideoTaskMapper.updateById(update);
            return responseText;
        } catch (Exception e) {
            throw new ServiceException("同步 vimax-agent 任务状态失败: " + e.getMessage());
        }
    }

    private String queryVimaxJobStatus(String jobId) {
        String url = trimEndSlash(vimaxAgentBaseUrl) + "/api/jobs/" + jobId;
        Request.Builder builder = new Request.Builder().url(url).get().addHeader("Accept", "application/json");
        if (vimaxAgentApiKey != null && !vimaxAgentApiKey.isBlank()) {
            builder.addHeader("Authorization", "Bearer " + vimaxAgentApiKey);
        }
        Request request = builder.build();
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

    private String downloadVimaxVideoToProfile(String jobId) {
        File downloadDir = new File(RuoYiConfig.getProfile(), "download/video");
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            throw new ServiceException("创建视频下载目录失败: " + downloadDir.getAbsolutePath());
        }

        String url = trimEndSlash(vimaxAgentBaseUrl) + "/api/jobs/" + jobId + "/download";
        Request.Builder builder = new Request.Builder().url(url).get();
        if (vimaxAgentApiKey != null && !vimaxAgentApiKey.isBlank()) {
            builder.addHeader("Authorization", "Bearer " + vimaxAgentApiKey);
        }
        Request request = builder.build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                ResponseBody err = response.body();
                String errText = err == null ? "" : err.string();
                throw new ServiceException("下载视频失败: " + response.code() + " - " + errText);
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ServiceException("下载视频失败: 响应体为空");
            }
            String fileName = "vimax_video_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + ".mp4";
            File targetFile = new File(downloadDir, fileName);
            Files.write(targetFile.toPath(), body.bytes());
            if (!targetFile.exists() || !targetFile.isFile() || targetFile.length() <= 0) {
                throw new ServiceException("下载视频失败: 文件落盘异常");
            }
            return "/profile/download/video/" + fileName;
        } catch (IOException e) {
            throw new ServiceException("下载视频失败: " + e.getMessage());
        }
    }

    private String finalizeHeaderIfNeeded(FurnitureVideoTaskDO task, String localVideoUrl) {
        if (task == null || task.getGenerationTaskId() == null) {
            return null;
        }
        FurnitureVideoGenerationTaskDO header = furnitureVideoGenerationTaskMapper.selectById(task.getGenerationTaskId());
        if (header == null) {
            return null;
        }
        if ("success".equalsIgnoreCase(header.getStatus())) {
            return header.getRemoteFinalVideoUrl();
        }

        File localFile = resolveProfilePathByLocalUrl(localVideoUrl).toFile();
        String remoteUrl = starhomeFileUrlUtils.toPublicFileUrl(localFile);

        FurnitureVideoGenerationTaskDO updateHeader = new FurnitureVideoGenerationTaskDO();
        updateHeader.setId(header.getId());
        updateHeader.setCurrentTaskCount(1);
        updateHeader.setStatus("success");
        updateHeader.setLocalFinalVideoUrl(localVideoUrl);
        updateHeader.setRemoteFinalVideoUrl(remoteUrl);
        updateHeader.setErrorMessage(null);
        updateHeader.setUpdateTime(LocalDateTime.now());
        furnitureVideoGenerationTaskMapper.updateById(updateHeader);

        furnitureUserBalanceAccountService.consume(header.getUserId(), resolveVideoConsumePrice(header));
        taskApiInvokeService.completeDeferredVideoUsageRecord(header.getId(), remoteUrl, "SUCCESS");
        return remoteUrl;
    }

    private void markHeaderFailedIfNeeded(Long generationTaskId, String reason) {
        if (generationTaskId == null) {
            return;
        }
        FurnitureVideoGenerationTaskDO header = furnitureVideoGenerationTaskMapper.selectById(generationTaskId);
        if (header == null) {
            return;
        }
        if ("success".equalsIgnoreCase(header.getStatus()) || "failed".equalsIgnoreCase(header.getStatus())) {
            return;
        }
        FurnitureVideoGenerationTaskDO updateHeader = new FurnitureVideoGenerationTaskDO();
        updateHeader.setId(generationTaskId);
        updateHeader.setStatus("failed");
        updateHeader.setErrorMessage(reason);
        updateHeader.setUpdateTime(LocalDateTime.now());
        furnitureVideoGenerationTaskMapper.updateById(updateHeader);
    }

    private String buildDownloadUrl(String jobId, String resultUrl) {
        String path = resultUrl;
        if (path == null || path.isBlank()) {
            path = "/api/jobs/" + jobId + "/download";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return trimEndSlash(vimaxAgentBaseUrl) + path;
    }

    private String mapVimaxStatusToLocal(String vimaxStatus) {
        if (vimaxStatus == null || vimaxStatus.isBlank()) {
            return "process";
        }
        if ("completed".equalsIgnoreCase(vimaxStatus)) {
            return "success";
        }
        if ("failed".equalsIgnoreCase(vimaxStatus)) {
            return "failed";
        }
        return "process";
    }

    private Date parseIsoDate(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            return null;
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(isoDateTime, ISO_LOCAL_DATE_TIME);
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            return null;
        }
    }

    private String getText(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String trimEndSlash(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private Path resolveProfilePathByLocalUrl(String localUrl) {
        String normalized = localUrl == null ? "" : localUrl;
        if (normalized.startsWith("/profile/")) {
            normalized = normalized.substring("/profile/".length());
        } else if (normalized.startsWith("profile/")) {
            normalized = normalized.substring("profile/".length());
        }
        return new File(RuoYiConfig.getProfile(), normalized).toPath();
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
