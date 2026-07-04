package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.starhome.domain.FurnitureConsumeConfigDO;
import com.ruoyi.starhome.domain.FurnitureVideoGenerationTaskDO;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageItemResp;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageResp;
import com.ruoyi.starhome.domain.dto.VimaxVideoCallbackRequest;
import com.ruoyi.starhome.mapper.FurnitureVideoGenerationTaskMapper;
import com.ruoyi.starhome.mapper.FurnitureVideoTaskMapper;
import com.ruoyi.starhome.service.IFurnitureConsumeConfigService;
import com.ruoyi.starhome.service.IFurnitureUserBalanceAccountService;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import com.ruoyi.starhome.service.IWechatNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FurnitureVideoTaskServiceImpl implements IFurnitureVideoTaskService {

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Value("${starhome.vimax-agent.base-url}")
    private String vimaxAgentBaseUrl;

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
    private IWechatNotifyService wechatNotifyService;


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
    public void handleVideoTaskCallback(VimaxVideoCallbackRequest request) {
        log.info("收到 vimax-agent 视频任务回调, request={}", request);
        if (request == null || request.getJobId() == null || request.getJobId().isBlank()) {
            throw new ServiceException("回调请求 job_id 不能为空");
        }

        String jobId = request.getJobId();
        log.info("收到 vimax-agent 视频任务回调, jobId={}, status={}", jobId, request.getStatus());

        FurnitureVideoTaskDO task = furnitureVideoTaskMapper.selectOne(
                new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                        .eq(FurnitureVideoTaskDO::getTaskId, jobId)
                        .last("limit 1")
        );
        if (task == null) {
            log.warn("回调收到未知任务, jobId={}", jobId);
            throw new ServiceException("未找到视频任务: " + jobId);
        }

        // 已完成的任务不再重复处理
        if (task.getIsComplete() != null && task.getIsComplete() == 1) {
            log.info("任务已完成，跳过回调处理, jobId={}, localStatus={}", jobId, task.getStatus());
            return;
        }

        String vimaxStatus = request.getStatus();
        String progress = request.getProgress();
        String error = request.getError();
        String resultUrl = request.getResultUrl();
        String agentPrompt = request.getPrompt();

        String downloadUrl = buildDownloadUrl(jobId, resultUrl);
        Date finishedAt = parseIsoDate(request.getFinishedAt());

        FurnitureVideoTaskDO update = new FurnitureVideoTaskDO();
        update.setId(task.getId());
        update.setProgress(progress);
        update.setFailReason(error);
        update.setVideoUrlRemote(downloadUrl);
        if (agentPrompt != null && !agentPrompt.isBlank()) {
            update.setPrompt(agentPrompt);
        }
        if (finishedAt != null) {
            update.setFinishTime(finishedAt);
        }

        try {
            if ("failed".equalsIgnoreCase(vimaxStatus)) {
                update.setStatus("failed");
                update.setIsComplete(1);
                furnitureVideoTaskMapper.updateById(update);
                String failReason = error == null || error.isBlank() ? "任务失败" : error;
                markHeaderFailedIfNeeded(task.getGenerationTaskId(), failReason);
                taskApiInvokeService.completeDeferredVideoUsageRecord(task.getGenerationTaskId(), null, "FAIL");
                sendVideoResultNotify(task, "失败");
                log.info("视频任务回调处理完成（失败）, jobId={}", jobId);
                return;
            }

            if ("completed".equalsIgnoreCase(vimaxStatus)) {
                update.setVideoUrlLocal(null);
                String finalRemoteUrl = finalizeHeaderIfNeeded(task, downloadUrl);
                update.setVideoUrlRemote(finalRemoteUrl);
                update.setIsComplete(1);
                update.setStatus("success");
                furnitureVideoTaskMapper.updateById(update);
                sendVideoResultNotify(task, "已完成");
                log.info("视频任务回调处理完成（成功）, jobId={}", jobId);
                return;
            }

            // 非终态（处理中），仅更新进度等信息
            update.setStatus("process");
            update.setIsComplete(0);
            furnitureVideoTaskMapper.updateById(update);
            log.info("视频任务回调处理完成（处理中）, jobId={}, progress={}", jobId, progress);
        } catch (Exception e) {
            log.error("处理视频任务回调异常, jobId={}", jobId, e);
            try {
                sendVideoResultNotify(task, "异常");
            } catch (Exception ignore) {
                log.error("发送视频生成异常微信通知失败, generationTaskId={}, userId={}",
                        task.getGenerationTaskId(), task.getUserId(), ignore);
            }
            throw new ServiceException("处理视频任务回调失败: " + e.getMessage());
        }
    }

    private String finalizeHeaderIfNeeded(FurnitureVideoTaskDO task, String remoteVideoUrl) {
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

        FurnitureVideoGenerationTaskDO updateHeader = new FurnitureVideoGenerationTaskDO();
        updateHeader.setId(header.getId());
        updateHeader.setCurrentTaskCount(1);
        updateHeader.setStatus("success");
        updateHeader.setLocalFinalVideoUrl(null);
        updateHeader.setRemoteFinalVideoUrl(remoteVideoUrl);
        updateHeader.setErrorMessage(null);
        updateHeader.setUpdateTime(LocalDateTime.now());
        furnitureVideoGenerationTaskMapper.updateById(updateHeader);

        furnitureUserBalanceAccountService.consume(header.getUserId(), resolveVideoConsumePrice(header));
        taskApiInvokeService.completeDeferredVideoUsageRecord(header.getId(), remoteVideoUrl, "SUCCESS");
        return remoteVideoUrl;
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

    /**
     * 发送视频生成结果统一微信通知
     *
     * @param task   视频子任务
     * @param result 处理结果（已完成 / 失败 / 异常）
     */
    private void sendVideoResultNotify(FurnitureVideoTaskDO task, String result) {
        try {
            // 查询工单名称
            String taskName = "图片生成视频";
            FurnitureVideoGenerationTaskDO header = furnitureVideoGenerationTaskMapper.selectById(task.getGenerationTaskId());
            if (header != null) {
                StringBuilder sb = new StringBuilder();
                if (header.getProduct() != null && !header.getProduct().isBlank()) {
                    sb.append(header.getProduct());
                }
                if (header.getMaterial() != null && !header.getMaterial().isBlank()) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(header.getMaterial());
                }
                if (sb.length() > 0) {
                    taskName = sb.toString();
                }
            }

            // 结束时间
            String finishTime = com.ruoyi.common.utils.StringUtils.substring(
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 0, 19);

            wechatNotifyService.notifyVideoResult(
                    task.getUserId(),
                    task.getGenerationTaskId(),
                    taskName,
                    finishTime,
                    result
            );
        } catch (Exception e) {
            log.error("发送视频结果微信通知失败, generationTaskId={}, userId={}",
                    task.getGenerationTaskId(), task.getUserId(), e);
        }
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
