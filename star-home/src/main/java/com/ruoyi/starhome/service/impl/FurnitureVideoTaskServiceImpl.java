package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.security.util.SecurityFrameworkUtils;
import com.ruoyi.starhome.domain.FurnitureNumberApiPoolDO;
import com.ruoyi.starhome.domain.FurnitureVideoGenerationTaskDO;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageItemResp;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageResp;
import com.ruoyi.starhome.domain.dto.ImageGenerateVideoRequest;
import com.ruoyi.starhome.enums.ConsumeConstants;
import com.ruoyi.starhome.mapper.FurnitureNumberApiPoolMapper;
import com.ruoyi.starhome.mapper.FurnitureVideoGenerationTaskMapper;
import com.ruoyi.starhome.mapper.FurnitureVideoTaskMapper;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
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
public class FurnitureVideoTaskServiceImpl implements IFurnitureVideoTaskService {

    private static final String IMAGE2IMAGE_T8STAR_API = "image2image_t8star_api";
    private static final String VIDEO_GENERATION_STATUS_URL = "https://ai.t8star.cn/v2/videos/generations/";
    private static final String STATUS_NOT_START = "NOT_START";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILURE = "FAILURE";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Autowired
    private FurnitureVideoTaskMapper furnitureVideoTaskMapper;

    @Autowired
    private FurnitureNumberApiPoolMapper furnitureNumberApiPoolMapper;

    @Autowired
    private FurnitureVideoGenerationTaskMapper furnitureVideoGenerationTaskMapper;

    @Autowired
    private ITaskApiInvokeService taskApiInvokeService;

    @Override
    public void processAppendingGenerationTasks() {
        List<FurnitureVideoGenerationTaskDO> appendingHeaders = furnitureVideoGenerationTaskMapper.selectList(
                new LambdaQueryWrapper<FurnitureVideoGenerationTaskDO>()
                        .eq(FurnitureVideoGenerationTaskDO::getStatus, "appending")
                        .orderByAsc(FurnitureVideoGenerationTaskDO::getId)
        );

        if (appendingHeaders == null || appendingHeaders.isEmpty()) {
            return;
        }

        for (FurnitureVideoGenerationTaskDO header : appendingHeaders) {
            if (header == null || header.getId() == null) {
                continue;
            }
            try {
                mergeHeaderVideosAndUpdateUrl(header);
            } catch (Exception e) {
                log.error("拼接视频失败, generationTaskId={}", header.getId(), e);
                FurnitureVideoGenerationTaskDO failUpdate = new FurnitureVideoGenerationTaskDO();
                failUpdate.setId(header.getId());
                failUpdate.setStatus("failed");
                failUpdate.setErrorMessage(e.getMessage());
                furnitureVideoGenerationTaskMapper.updateById(failUpdate);
            }
        }
    }

    private void mergeHeaderVideosAndUpdateUrl(FurnitureVideoGenerationTaskDO header) {
        List<FurnitureVideoTaskDO> detailList = furnitureVideoTaskMapper.selectList(
                new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                        .eq(FurnitureVideoTaskDO::getGenerationTaskId, header.getId())
                        .orderByAsc(FurnitureVideoTaskDO::getStartTime)
                        .orderByAsc(FurnitureVideoTaskDO::getId)
        );

        if (detailList == null || detailList.isEmpty()) {
            throw new ServiceException("单据头下无可拼接明细");
        }

        List<String> localSegmentUrls = detailList.stream()
                .filter(item -> item.getVideoUrlLocal() != null && !item.getVideoUrlLocal().isBlank())
                .map(FurnitureVideoTaskDO::getVideoUrlLocal)
                .collect(Collectors.toList());

        if (localSegmentUrls.isEmpty()) {
            throw new ServiceException("单据头下无可拼接本地视频");
        }

        String mergedLocalUrl = mergeLocalMp4Videos(localSegmentUrls);

        FurnitureVideoGenerationTaskDO successUpdate = new FurnitureVideoGenerationTaskDO();
        successUpdate.setId(header.getId());
        successUpdate.setLocalFinalVideoUrl(mergedLocalUrl);
        successUpdate.setStatus("success");
        successUpdate.setErrorMessage(null);
        furnitureVideoGenerationTaskMapper.updateById(successUpdate);
    }

    private String mergeLocalMp4Videos(List<String> localSegmentUrls) {
        File downloadDir = new File(RuoYiConfig.getProfile(), "download/video");
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            throw new ServiceException("创建视频下载目录失败: " + downloadDir.getAbsolutePath());
        }

        List<Path> localPaths = new ArrayList<>();
        for (String localUrl : localSegmentUrls) {
            Path path = resolveProfilePathByLocalUrl(localUrl);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new ServiceException("待拼接视频文件不存在: " + path);
            }
            localPaths.add(path);
        }

        Path listFile = null;
        try {
            listFile = Files.createTempFile("ffmpeg_concat_", ".txt");
            List<String> lines = localPaths.stream()
                    .map(path -> "file '" + path.toAbsolutePath().toString().replace("\\", "/") + "'")
                    .collect(Collectors.toList());
            log.info("localPaths:{}",lines);
            Files.write(listFile, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            String outputName = "video_merged_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + ".mp4";
            File outputFile = new File(downloadDir, outputName);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-f", "concat", "-safe", "0",
                    "-i", listFile.toAbsolutePath().toString(),
                    "-c", "copy",
                    outputFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String ffmpegOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0 || !outputFile.exists() || outputFile.length() <= 0) {
                throw new ServiceException("ffmpeg拼接失败: " + ffmpegOutput);
            }

            return "/profile/download/video/" + outputName;
        } catch (IOException e) {
            throw new ServiceException("执行ffmpeg拼接异常: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("ffmpeg拼接被中断: " + e.getMessage());
        } finally {
            if (listFile != null) {
                try {
                    Files.deleteIfExists(listFile);
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    private Path resolveProfilePathByLocalUrl(String localUrl) {
        String normalized = localUrl;
        if (normalized.startsWith("/profile/")) {
            normalized = normalized.substring("/profile/".length());
        } else if (normalized.startsWith("profile/")) {
            normalized = normalized.substring("profile/".length());
        }
        return new File(RuoYiConfig.getProfile(), normalized).toPath();
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
    @Transactional
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
        FurnitureVideoTaskDO updatedTask = updateVideoTaskByResponse(taskId, responseText);
        handleFailedVideoTaskIfNeeded(updatedTask);
        handleNextVideoSegmentIfNeeded(updatedTask);
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

    private FurnitureVideoTaskDO updateVideoTaskByResponse(String taskId, String responseText) {
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

            furnitureVideoTaskMapper.update(update, new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                            .eq(FurnitureVideoTaskDO::getTaskId, taskId));

            if (isSuccessStatus(update.getStatus()) && remoteVideoUrl != null && !remoteVideoUrl.isBlank()) {
                asyncDownloadVideoAndUpdateLocalUrl(taskId, remoteVideoUrl);
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

    private void handleFailedVideoTaskIfNeeded(FurnitureVideoTaskDO currentTask) {
        if (currentTask == null || !isFailedStatus(currentTask.getStatus())) {
            return;
        }
        retryFailedVideoTask(currentTask);
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
        retryReq.setImageUrls(resolveRetryImageUrls(generationTask, currentTask));
        retryReq.setConsumeConstants(ConsumeConstants.IMAGE2VIDEO);
        if (retryReq.getImageUrls() == null || retryReq.getImageUrls().isEmpty()) {
            log.warn("视频任务失败后重试被跳过, 未找到有效入参图片, taskId={}", currentTask.getTaskId());
            return;
        }

        try {
            taskApiInvokeService.imageGenerateVideo(retryReq);

            FurnitureVideoTaskDO retryUpdate = new FurnitureVideoTaskDO();
            retryUpdate.setStatus("FAIL");
            retryUpdate.setFailReason(null);
            retryUpdate.setProgress("0%");
            retryUpdate.setIsComplete(0);
            retryUpdate.setFinishTime(null);
            furnitureVideoTaskMapper.update(retryUpdate,
                    new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                            .eq(FurnitureVideoTaskDO::getId, currentTask.getId()));

            FurnitureVideoGenerationTaskDO headerUpdate = new FurnitureVideoGenerationTaskDO();
            headerUpdate.setId(generationTask.getId());
            headerUpdate.setStatus("process");
            headerUpdate.setErrorMessage(null);
            furnitureVideoGenerationTaskMapper.updateById(headerUpdate);
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

    private void handleNextVideoSegmentIfNeeded(FurnitureVideoTaskDO currentTask) {
        if (currentTask == null || !isSuccessStatus(currentTask.getStatus()) || currentTask.getGenerationTaskId() == null) {
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

        if (expected == current + 1) {
            FurnitureVideoGenerationTaskDO updateHeader = new FurnitureVideoGenerationTaskDO();
            updateHeader.setId(generationTask.getId());
            updateHeader.setCurrentTaskCount(current + 1);
            updateHeader.setStatus("appending");
            furnitureVideoGenerationTaskMapper.updateById(updateHeader);
            return;
        }

        if (expected > current + 1) {
            triggerNextVideoGeneration(generationTask, currentTask, current);
        }
    }

    private void triggerNextVideoGeneration(FurnitureVideoGenerationTaskDO generationTask, FurnitureVideoTaskDO currentTask, int currentTaskCount) {
        String videoUrl = currentTask.getVideoUrlRemote();
        if (videoUrl == null || videoUrl.isBlank()) {
            return;
        }

        ImageGenerateVideoRequest nextReq = new ImageGenerateVideoRequest();
        nextReq.setGenerationTaskId(generationTask.getId());
        nextReq.setUserId(generationTask.getUserId() != null ? generationTask.getUserId() : currentTask.getUserId());
        nextReq.setImageUrls(Collections.singletonList(videoUrl));
        nextReq.setProduct(generationTask.getProduct());
        nextReq.setMaterial(generationTask.getMaterial());
        nextReq.setPrompt(resolveNextPrompt(generationTask, currentTask));
        nextReq.setConsumeConstants(ConsumeConstants.IMAGE2VIDEO);
        try {
            taskApiInvokeService.imageGenerateVideo(nextReq);

            FurnitureVideoGenerationTaskDO updateHeader = new FurnitureVideoGenerationTaskDO();
            updateHeader.setId(generationTask.getId());
            updateHeader.setCurrentTaskCount(currentTaskCount + 1);
            updateHeader.setStatus("process");
            furnitureVideoGenerationTaskMapper.updateById(updateHeader);
        } catch (Exception e) {
            FurnitureVideoGenerationTaskDO failedHeader = new FurnitureVideoGenerationTaskDO();
            failedHeader.setId(generationTask.getId());
            failedHeader.setStatus("failed");
            failedHeader.setErrorMessage(e.getMessage());
            furnitureVideoGenerationTaskMapper.updateById(failedHeader);
            log.error("发起下一段视频生成失败, generationTaskId={}, taskId={}", generationTask.getId(), currentTask.getTaskId(), e);
        }
    }

    private String resolveNextPrompt(FurnitureVideoGenerationTaskDO generationTask, FurnitureVideoTaskDO currentTask) {
        if (currentTask.getPrompt() != null && !currentTask.getPrompt().isBlank()) {
            return currentTask.getPrompt();
        }

        String product = generationTask == null || generationTask.getProduct() == null || generationTask.getProduct().isBlank()
                ? "家居产品" : generationTask.getProduct();
        String material = generationTask == null || generationTask.getMaterial() == null || generationTask.getMaterial().isBlank()
                ? "原木" : generationTask.getMaterial();

        return "视频二（8——16s）：\n" +
                "生成高端家居商业视频，8K，HDR，60fps，暖色自然光，无水印无文字。\n" +
                "\n" +
                "主体：" + product + "（材质" + material + "），保持完全一致空间。\n" +
                "\n" +
                "【时间轴控制】\n" +
                "\n" +
                "8s（必须完全匹配上一段）:\n" +
                "固定镜头：\n" +
                "- 中近景（medium-close shot）\n" +
                "- 平视角度\n" +
                "- 产品正中心\n" +
                "- 无运动\n" +
                "- 光线完全一致\n" +
                "\n" +
                "9s:\n" +
                "镜头开始极慢速运动（与上一段相同速度），向侧前方轻微移动\n" +
                "\n" +
                "10s:\n" +
                "转为轻微环绕（orbit），幅度极小，产品仍居中\n" +
                "\n" +
                "11s:\n" +
                "展示材质细节（" + material + "纹理变化），光影自然滑动\n" +
                "\n" +
                "12s:\n" +
                "人物再次轻微参与（如手滑过表面），不遮挡主体\n" +
                "\n" +
                "13s:\n" +
                "镜头继续环绕 + 微推进（组合运动，但速度恒定）\n" +
                "\n" +
                "14s:\n" +
                "人物退出，画面回归纯产品展示\n" +
                "\n" +
                "15s:\n" +
                "镜头减速，准备收尾\n" +
                "\n" +
                "16s:\n" +
                "定格画面：\n" +
                "产品中心构图，光影停留在设计亮点\n" +
                "\n" +
                "要求：\n" +
                "same camera height, same focal length, same speed\n" +
                "no scene change, no lighting change";
    }

    private boolean isSuccessStatus(String status) {
        return status != null && STATUS_SUCCESS.equalsIgnoreCase(status.trim());
    }

    private boolean isFailedStatus(String status) {
        return status != null && STATUS_FAILURE.equalsIgnoreCase(status.trim());
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
                    String localUrl = downloadRemoteVideoToProfile(remoteVideoUrl);
                    if (localUrl == null || localUrl.isBlank()) {
                        return;
                    }
                    FurnitureVideoTaskDO localUpdate = new FurnitureVideoTaskDO();
                    localUpdate.setVideoUrlLocal(localUrl);
                    furnitureVideoTaskMapper.update(localUpdate,
                            new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                                    .eq(FurnitureVideoTaskDO::getTaskId, taskId));
                } catch (Exception e) {
                    log.error("异步下载视频并更新本地地址失败, taskId={}, remoteUrl={}", taskId, remoteVideoUrl, e);
                }
            }
        });
    }

    private String downloadRemoteVideoToProfile(String remoteVideoUrl) {
        File downloadDir = new File(RuoYiConfig.getProfile(), "download/video");
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            throw new ServiceException("创建视频下载目录失败: " + downloadDir.getAbsolutePath());
        }

        Request request = new Request.Builder()
                .url(remoteVideoUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceException("下载远程视频失败: " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ServiceException("下载远程视频失败: 响应体为空");
            }

            String ext = resolveVideoExtByMimeType(response.header("Content-Type"));
            String fileName = "video_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            File targetFile = new File(downloadDir, fileName);
            Files.write(targetFile.toPath(), body.bytes());

            if (!targetFile.exists() || !targetFile.isFile()) {
                throw new ServiceException("下载远程视频失败: 文件落盘异常");
            }
            return "/profile/download/video/" + fileName;
        } catch (IOException e) {
            throw new ServiceException("下载远程视频失败: " + e.getMessage());
        }
    }

    private String resolveVideoExtByMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "mp4";
        }
        String normalized = mimeType.toLowerCase();
        if (normalized.contains("quicktime")) {
            return "mov";
        }
        if (normalized.contains("webm")) {
            return "webm";
        }
        if (normalized.contains("x-matroska") || normalized.contains("matroska")) {
            return "mkv";
        }
        if (normalized.contains("avi")) {
            return "avi";
        }
        return "mp4";
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
