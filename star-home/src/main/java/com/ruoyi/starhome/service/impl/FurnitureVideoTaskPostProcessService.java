package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.starhome.domain.FurnitureVideoGenerationTaskDO;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.domain.dto.ImageGenerateVideoRequest;
import com.ruoyi.starhome.enums.ConsumeConstants;
import com.ruoyi.starhome.mapper.FurnitureVideoGenerationTaskMapper;
import com.ruoyi.starhome.mapper.FurnitureVideoTaskMapper;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import com.ruoyi.starhome.util.StarhomeFileUrlUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FurnitureVideoTaskPostProcessService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILURE = "FAILURE";
    private static final String STATUS_FAIL = "FAIL";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Autowired
    private FurnitureVideoTaskMapper furnitureVideoTaskMapper;

    @Autowired
    private FurnitureVideoGenerationTaskMapper furnitureVideoGenerationTaskMapper;

    @Autowired
    private ITaskApiInvokeService taskApiInvokeService;

    @Autowired
    private StarhomeFileUrlUtils starhomeFileUrlUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FurnitureVideoTaskDO updateVideoTaskByResponse(String taskId, String responseText) {
        log.info("begin updateVideoTaskByResponse responseText:{}",responseText);
        try {
            FurnitureVideoTaskDO existingTask = furnitureVideoTaskMapper.selectOne(
                    new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                            .eq(FurnitureVideoTaskDO::getTaskId, taskId)
                            .last("limit 1"));

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
            String remoteVideoUrl = getText(dataNode, "output", null);
            update.setVideoUrlRemote(remoteVideoUrl);

            Long finishTimeSeconds = getLong(root, "finish_time");
            if (finishTimeSeconds == null || finishTimeSeconds <= 0) {
                finishTimeSeconds = getLong(dataNode, "completed_at");
            }
            if (finishTimeSeconds != null && finishTimeSeconds > 0) {
                update.setFinishTime(new Date(finishTimeSeconds * 1000));
            }

            update.setIsComplete(isCompletedStatus(update.getStatus()) ? 1 : 0);

            int updateCount = furnitureVideoTaskMapper.update(update, new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                    .eq(FurnitureVideoTaskDO::getTaskId, taskId));
            log.info("updateVideoTaskByResponse taskId={} updateCount={}", taskId, updateCount);
            updateGenerationTaskCountIfNeeded(existingTask, update);

            if (isSuccessStatus(update.getStatus()) && remoteVideoUrl != null && !remoteVideoUrl.isBlank()) {
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            asyncDownloadVideoAndUpdateLocalUrl(taskId, remoteVideoUrl);
                        }
                    });
                } else {
                    asyncDownloadVideoAndUpdateLocalUrl(taskId, remoteVideoUrl);
                }
            }

            return furnitureVideoTaskMapper.selectOne(
                    new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                            .eq(FurnitureVideoTaskDO::getTaskId, taskId)
                            .last("limit 1"));
        } catch (Exception e) {
            log.error("更新视频任务进度失败, taskId={}, resp={}", taskId, responseText, e);
            throw new ServiceException("更新视频任务进度失败: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedVideoTaskIfNeeded(FurnitureVideoTaskDO currentTask) {
        if (currentTask == null || currentTask.getIsComplete() == 0 || !isFailedStatus(currentTask.getStatus())) {
            return;
        }
        retryFailedVideoTask(currentTask);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNextVideoSegmentIfNeeded(FurnitureVideoTaskDO currentTask) {
        if (currentTask == null || currentTask.getIsComplete() == 0 || !isSuccessStatus(currentTask.getStatus()) || currentTask.getGenerationTaskId() == null) {
            return;
        }

        FurnitureVideoGenerationTaskDO generationTask = furnitureVideoGenerationTaskMapper.selectById(currentTask.getGenerationTaskId());
        if (generationTask == null) {
            return;
        }

        Integer expectedTaskCount = generationTask.getExpectedTaskCount();
        Integer currentTaskCount = generationTask.getCurrentTaskCount();
        int expected = expectedTaskCount == null ? 0 : expectedTaskCount;
        int current = currentTaskCount == null ? 0 : currentTaskCount;

        if (expected > current) {
            triggerNextVideoGeneration(generationTask, currentTask);
        }
    }

    private void updateGenerationTaskCountIfNeeded(FurnitureVideoTaskDO existingTask, FurnitureVideoTaskDO updateTask) {
        if (existingTask == null || existingTask.getGenerationTaskId() == null) {
            return;
        }
        if (!isSuccessStatus(updateTask.getStatus())) {
            return;
        }

        String oldStatus = existingTask.getStatus();
        if (isSuccessStatus(oldStatus)) {
            return;
        }

        FurnitureVideoGenerationTaskDO generationTask = furnitureVideoGenerationTaskMapper.selectById(existingTask.getGenerationTaskId());
        if (generationTask == null) {
            return;
        }

        Integer currentTaskCount = generationTask.getCurrentTaskCount();
        Integer expectedTaskCount = generationTask.getExpectedTaskCount();
        int current = currentTaskCount == null ? 0 : currentTaskCount;
        int expected = expectedTaskCount == null ? 0 : expectedTaskCount;

        FurnitureVideoGenerationTaskDO updateHeader = new FurnitureVideoGenerationTaskDO();
        updateHeader.setId(generationTask.getId());
        updateHeader.setCurrentTaskCount(current + 1);
        if (expected > 0 && current + 1 >= expected) {
            updateHeader.setStatus("appending");
        }
        furnitureVideoGenerationTaskMapper.updateById(updateHeader);
    }

    private void retryFailedVideoTask(FurnitureVideoTaskDO currentTask) {
        if (currentTask.getGenerationTaskId() == null) {
            return;
        }

        FurnitureVideoGenerationTaskDO generationTask = furnitureVideoGenerationTaskMapper.selectById(currentTask.getGenerationTaskId());
        if (generationTask == null) {
            return;
        }

        ImageGenerateVideoRequest retryReq = new ImageGenerateVideoRequest();
        retryReq.setGenerationTaskId(generationTask.getId());
        retryReq.setUserId(generationTask.getUserId() != null ? generationTask.getUserId() : currentTask.getUserId());
        retryReq.setProduct(generationTask.getProduct());
        retryReq.setMaterial(generationTask.getMaterial());
        retryReq.setPrompt(currentTask.getPrompt());
        retryReq.setImageUrls(Collections.singletonList(currentTask.getImageUrl()));
//        retryReq.setImageUrls(resolveRetryImageUrls(generationTask, currentTask));
        log.info("retry failed video task, retryReq={}", retryReq);
        retryReq.setConsumeConstants(ConsumeConstants.IMAGE2VIDEO);
        if (retryReq.getImageUrls() == null || retryReq.getImageUrls().isEmpty()) {
            log.warn("视频任务失败后重试被跳过, 未找到有效入参图片, taskId={}", currentTask.getTaskId());
            return;
        }

        try {
            taskApiInvokeService.imageGenerateVideo(retryReq);
        } catch (Exception e) {
            log.error("视频任务失败后重试触发失败, generationTaskId={}, taskId={}", generationTask.getId(), currentTask.getTaskId(), e);
        }
    }

    private List<String> resolveRetryImageUrls(FurnitureVideoGenerationTaskDO generationTask, FurnitureVideoTaskDO currentTask) {
        Integer currentTaskCount = generationTask.getCurrentTaskCount();
        int current = currentTaskCount == null ? 0 : currentTaskCount;

        if (current <= 0) {
            if (currentTask.getImageUrl() == null || currentTask.getImageUrl().isBlank()) {
                return Collections.emptyList();
            }
            return Arrays.stream(currentTask.getImageUrl().split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .collect(Collectors.toList());
        }

        FurnitureVideoTaskDO latestSuccessTask = furnitureVideoTaskMapper.selectOne(
                new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                        .eq(FurnitureVideoTaskDO::getGenerationTaskId, generationTask.getId())
                        .eq(FurnitureVideoTaskDO::getIsComplete, 1)
                        .in(FurnitureVideoTaskDO::getStatus, "SUCCESS", "SUCCEEDED", "COMPLETED")
                        .orderByDesc(FurnitureVideoTaskDO::getId)
                        .last("limit 1")
        );
        if (latestSuccessTask == null || latestSuccessTask.getVideoUrlRemote() == null || latestSuccessTask.getVideoUrlRemote().isBlank()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(latestSuccessTask.getVideoUrlRemote());
    }

    private void triggerNextVideoGeneration(FurnitureVideoGenerationTaskDO generationTask, FurnitureVideoTaskDO currentTask) {
        log.info("generationTaskId={}, currentTaskId={},生成下一条视频", generationTask.getId(), currentTask.getId());
        String videoUrl = currentTask.getVideoUrlRemote();
        if (videoUrl == null || videoUrl.isBlank()) {
            return;
        }

        String lastFrameImageUrl;
        try {
            lastFrameImageUrl = extractLastFrameToProfile(videoUrl);
        } catch (Exception e) {
            FurnitureVideoGenerationTaskDO failedHeader = new FurnitureVideoGenerationTaskDO();
            failedHeader.setId(generationTask.getId());
            failedHeader.setStatus("failed");
            failedHeader.setErrorMessage("提取视频最后一帧失败: " + e.getMessage());
            furnitureVideoGenerationTaskMapper.updateById(failedHeader);
            log.error("发起下一段视频生成失败(提取最后一帧异常), generationTaskId={}, taskId={}, videoUrl={}",
                    generationTask.getId(), currentTask.getTaskId(), videoUrl, e);
            return;
        }

        ImageGenerateVideoRequest nextReq = new ImageGenerateVideoRequest();
        nextReq.setGenerationTaskId(generationTask.getId());
        nextReq.setUserId(generationTask.getUserId() != null ? generationTask.getUserId() : currentTask.getUserId());
        nextReq.setImageUrls(Collections.singletonList(lastFrameImageUrl));
        nextReq.setProduct(generationTask.getProduct());
        nextReq.setMaterial(generationTask.getMaterial());
        nextReq.setPrompt(resolveNextPrompt(generationTask));
        nextReq.setConsumeConstants(ConsumeConstants.IMAGE2VIDEO);
        try {
            taskApiInvokeService.imageGenerateVideo(nextReq);

            FurnitureVideoGenerationTaskDO updateHeader = new FurnitureVideoGenerationTaskDO();
            updateHeader.setId(generationTask.getId());
            updateHeader.setStatus("process");
            furnitureVideoGenerationTaskMapper.updateById(updateHeader);
        } catch (Exception e) {
            FurnitureVideoGenerationTaskDO failedHeader = new FurnitureVideoGenerationTaskDO();
            failedHeader.setId(generationTask.getId());
            failedHeader.setStatus("failed");
            failedHeader.setErrorMessage(e.getMessage());
            furnitureVideoGenerationTaskMapper.updateById(failedHeader);
            log.error("发起下一段视频生成失败, generationTaskId={}, taskId={}, imageUrl={}",
                    generationTask.getId(), currentTask.getTaskId(), lastFrameImageUrl, e);
        }
    }

    private String extractLastFrameToProfile(String remoteVideoUrl) {
        File frameDir = new File(RuoYiConfig.getProfile(), "download/video-frame");
        if (!frameDir.exists() && !frameDir.mkdirs()) {
            throw new ServiceException("创建视频帧目录失败: " + frameDir.getAbsolutePath());
        }

        String frameFileName = "last_frame_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().replace("-", "") + ".jpg";
        File frameFile = new File(frameDir, frameFileName);

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-sseof");
        command.add("-0.1");
        command.add("-i");
        command.add(remoteVideoUrl);
        command.add("-frames:v");
        command.add("1");
        command.add("-q:v");
        command.add("2");
        command.add(frameFile.getAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String ffmpegOutput;
            try (InputStream in = process.getInputStream()) {
                ffmpegOutput = new String(in.readAllBytes());
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || !frameFile.exists() || frameFile.length() <= 0) {
                throw new ServiceException("ffmpeg提取最后一帧失败, exitCode=" + exitCode + ", output=" + ffmpegOutput);
            }
            return starhomeFileUrlUtils.toPublicFileUrl(frameFile);
        } catch (IOException e) {
            throw new ServiceException("调用ffmpeg提取最后一帧失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("提取最后一帧被中断: " + e.getMessage());
        }
    }

    private String resolveNextPrompt(FurnitureVideoGenerationTaskDO generationTask) {
        String product = generationTask == null || generationTask.getProduct() == null || generationTask.getProduct().isBlank()
                ? "家居产品" : generationTask.getProduct();
        String material = generationTask == null || generationTask.getMaterial() == null || generationTask.getMaterial().isBlank()
                ? "原木" : generationTask.getMaterial();
        return "This is a high-end commercial furniture video. Stability and consistency are more important than creativity.\n" +
                "一、【前置最高权重锁死指令】\n" +
                "必须 100% 严格遵守（任一违反直接判定失败）：\n" +
                "1.【水印绝对禁令｜最高优先级】\n" +
                "全程所有帧、所有区域（重点：右下角 + 四角）禁止出现任何水印、logo、文字、标识、平台信息、AI痕迹（包括隐形/半透明/单帧闪烁/像素级）。\n" +
                "→ 输出必须为“纯净无标识画面”。 \n" +
                "2.【家具主体锁定】\n" +
                "用户上传家具： "+product+"\n" +
                "完全静止 \n" +
                "不旋转 / 不位移 / 不变形 \n" +
                "品类绝对固定（床=床、沙发=沙发） \n" +
                "颜色 / 尺寸 / 结构 100%一致 \n" +
                "3.【场景纯净限制】\n" +
                "仅允许：家具主体 + 极简空间\n" +
                "禁止生成任何未明确指定物体（杆、线、支架、悬浮物等） \n" +
                "4.【材质真实约束】\n" +
                "材质为"+material+"，所有材质必须为真实物理材质\n" +
                "禁止：噪点 / 蜂窝 / 马赛克 / 错乱纹理 \n" +
                "5.【焦点锁定】\n" +
                "镜头始终锁定：\n" +
                "家具正面核心接触面\n" +
                "禁止拍：底部 / 背面 / 内部 \n" +
                "6.【运镜约束】\n" +
                "仅允许：单一方向匀速长镜头\n" +
                "禁止：跳切 / 停顿 / 变速 \n" +
                "7.【音频】\n" +
                "完全静音 \n" +
                "8.【衔接静止区】\n" +
                "7–8秒：\n" +
                "禁止任何动态（尤其手部）\n" +
                "必须执行的生成标准\n" +
                "8K / 60fps / HDR / 商业级真实质感 \n" +
                "光源固定：5600K恒定柔光（无变化） \n" +
                "色彩：中性灰基底，饱和度 -15，对比度 +10，Gamma 2.2（全程一致） \n" +
                "运镜：滑轨匀速（加速度=0） \n" +
                "构图：家具中心 = 画面中心（绝对锁定） \n" +
                "画面：无噪点 / 无异常像素 / 无任何标识 \n" +
                "二、通用全局规范\n" +
                "5.主体 = 用户上传家具（1:1复刻） \n" +
                "6.核心展示 = 正面接触面 \n" +
                "7.背景 = 全程不变\n" +
                "8.时长 = 8秒（严格）\n" +
                "三、分镜\n" +
                "0–4秒（推镜 + 对焦）\n" +
                "起始帧 = 用户原图（强制一致） \n" +
                "正向匀速推镜 \n" +
                "焦点：背景 → 家具核心面（平滑过渡）\n" +
                "4–6秒（横移）\n" +
                "右 → 左 匀速 \n" +
                "展示材质细节 \n" +
                "焦点始终锁定核心面\n" +
                "6–8秒（拉镜）\n" +
                "缓慢拉远 \n" +
                "回到完整家具构图 \n" +
                "最终帧 = 再次精确还原原始图片";
    }

    private boolean isSuccessStatus(String status) {
        return status != null && STATUS_SUCCESS.equalsIgnoreCase(status.trim());
    }

    private boolean isFailedStatus(String status) {
        return status != null && (STATUS_FAILURE.equalsIgnoreCase(status.trim()) || STATUS_FAIL.equalsIgnoreCase(status.trim()));
    }

    private boolean isCompletedStatus(String status) {
        return isSuccessStatus(status) || isFailedStatus(status);
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

    private void asyncDownloadVideoAndUpdateLocalUrl(String taskId, String remoteVideoUrl) {
        AsyncManager.me().execute(new TimerTask() {
            @Override
            public void run() {
                try {
                    String localUrl = starhomeFileUrlUtils.downloadRemoteVideoToProfile(remoteVideoUrl);
                    if (localUrl == null || localUrl.isBlank()) {
                        return;
                    }
                    updateLocalVideoUrlWithRetry(taskId, localUrl);
                } catch (Exception e) {
                    log.error("异步下载视频并更新本地地址失败, taskId={}, remoteUrl={}", taskId, remoteVideoUrl, e);
                }
            }
        });
    }

    private void updateLocalVideoUrlWithRetry(String taskId, String localUrl) {
        int maxRetry = 3;
        for (int i = 1; i <= maxRetry; i++) {
            try {
                FurnitureVideoTaskDO localUpdate = new FurnitureVideoTaskDO();
                localUpdate.setVideoUrlLocal(localUrl);
                int updated = furnitureVideoTaskMapper.update(localUpdate,
                        new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                                .eq(FurnitureVideoTaskDO::getTaskId, taskId)
                                .isNull(FurnitureVideoTaskDO::getVideoUrlLocal));
                if (updated > 0) {
                    return;
                }
                log.info("异步更新videoUrlLocal跳过，可能已被其他线程更新, taskId={}", taskId);
                return;
            } catch (Exception e) {
                if (!isLockTimeoutException(e) || i == maxRetry) {
                    throw e;
                }
                long sleepMs = i * 200L;
                log.warn("更新videoUrlLocal遇到锁等待，准备重试, taskId={}, retry={}/{}", taskId, i, maxRetry, e);
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new ServiceException("更新videoUrlLocal重试被中断: " + ex.getMessage());
                }
            }
        }
    }

    private boolean isLockTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("Lock wait timeout exceeded")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
