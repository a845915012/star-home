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
        return "【最高优先级・不可逾越铁则】（两段视频必须 100% 严格遵守，生成权重拉满）\n" +
                "无多余不明物体强制禁令：全程全帧画面（含背景、边角、景深过渡区间、镜头切换区间），绝对禁止生成任何用户未指定的、无意义的不明物体，包括但不限于莫名其妙的杆子、金属条、线条、立柱、支架、不明结构、悬浮物体、穿模的家具部件、多余的陈设品、非用户上传的额外家具；画面内仅保留用户上传图片中的家具主体、固定统一的极简背景空间场景，无任何额外的、多余的、不明的物体出现，镜头切换、景别变化、景深过渡时必须严格遵守。 \n" +
                "家具主体形态全程恒定铁则："+product+"从第一帧到最后一帧，品类、形态、结构、尺寸、朝向、颜色、材质 100% 与用户上传图片完全一致，全程恒定不变。沙发全程为沙发，床全程为床，床垫全程为床垫，绝对禁止跨品类替换、形态变形、结构重构，无任何穿模、形变、错位。 \n" +
                "家具全程绝对静止铁则：家具主体在空间中全程保持绝对静止，仅镜头做专业电影级滑轨匀速运动，无任何旋转、翻转、位移、开合、伸缩等动态变化，严格遵循现实物理规则，彻底消除 AI 违和感。 \n" +
                "材质纹理防错乱铁则：\n" +
                "所有材质特写必须还原真实物理纹理，绝对禁止生成蜂窝状、颗粒状、马赛克式、网格状的错乱噪点与畸变纹理\n" +
                "床垫面料：还原针织 / 天丝面料的细腻自然编织纹理，纤维走向清晰自然，无错乱填充\n" +
                "沙发材质：真皮还原自然细腻的皮革毛孔与不规则皮纹，布艺还原经纬分明的编织纹理，无畸变\n" +
                "木质 / 石材 / 金属材质：还原对应材质的原生自然肌理，无 AI 生成的虚假错乱纹理\n" +
                "若用户上传图片细节不清晰，默认生成对应品类家具的行业标准真实材质纹理，禁止用蜂窝噪点做模糊填充\n" +
                "零水印强制禁令：全程全帧画面无任何形式的水印、logo、标识、文字、角标，包括隐形水印、半透明标识、像素级水印，画面全程纯净无多余元素。 \n" +
                "无动态元素衔接禁令：衔接关键帧区间（第一段 7-8 秒、第二段 0-1 秒）绝对禁止出现模特及任何动态物体，家具全程保持静止，背景空间无任何变动，确保无缝拼接无断点、无新增不明物体。\n" +
                "\n" +
                "【固定参数锁死规范（无缝衔接核心）】\n" +
                "主体还原规则：严格 1:1 还原用户上传图片中的家具外观，画面绝对核心为 **"+product+"，精准呈现"+material+"的真实物理肌理** 核心要求，AI 不得做任何自主创意修改、形态重构。 \n" +
                "空间与视觉锚定：家具主体的几何中心，全程严格锁定在画面绝对中心轴线上，无论运镜如何变化，家具中心永远与画面中心重合，无任何偏移，空间结构、软装配色、陈设位置全程固定，无任何场景元素变动。 \n" +
                "画质与光影锁死：8K 超高清分辨率，60 帧 / 秒高帧率，HDR 广色域，真人实拍级商业宣传片质感；全程采用5600K 标准日光恒定柔光系统，主光、辅光、轮廓光的位置、强度、色温全程固定无变化，无过曝、无暗部死黑、无明暗起伏、无动态光影跳变，画面通透有层次，光影柔和不生硬，避免材质纹理出现反光畸变，杜绝光影错乱生成虚假线条 / 杆子。\n" +
                "色彩与参数统一：全程采用潘通中性灰基底，色彩饱和度统一 - 15，对比度统一 + 10，伽马值 2.2，全程无任何色彩偏移、滤镜变动；全程无任何人声配音、背景音乐、环境音效，纯视觉画面呈现。 \n" +
                "运镜铁则：全程采用专业电影级滑轨单组连贯匀速运镜，加速度为 0，绝对匀速，无任何镜头启停、无运镜方向突变、无景别跳变、无抖动、无卡顿、无急推急拉，完全模拟真人实拍的丝滑长镜头运动，单条 8 秒视频仅保留 2-3 组平缓运镜过渡，两段视频运镜节奏完全同频，运镜全程严格遵循透视规则，无错乱透视生成虚假线条 / 杆子。 \n" +
                "\n" +
                "0-3 秒 秒画面100% 承接所有强制锁死规范，起始帧与第一段结尾帧的锚点、光影、色彩、机位、虚化程度、背景空间结构 100% 完全匹配；采用与家具中心轴线垂直的正向轨道匀速微推长镜头，全程单方向、单速度、无任何启停切换，运镜速度与第一段完全同频；同步启动逆向景深平滑对焦，焦点从背景空间，以恒定速度平缓顺滑地向画面中心绝对静止的家具主体转移，3 秒末完成对焦，焦点 100% 锁定家具主体，背景回归柔和虚化；全程背景空间固定不变，无任何新增物体、无任何结构改动，绝对无莫名杆子、线条、不明结构出现；家具全程形态、结构、品类 100% 恒定不变，无任何变形、替换；全程无变速、无跳切，景深过渡完全掩盖拼接痕迹，画面全程无水印、无多余物体\n" +
                "3-6 秒镜头平缓切换为与家具正面平行的横向长距离匀速滑轨平移（右→左）长镜头，全程单方向、单速度、无任何启停切换，平移速度与前序推镜速度完全匹配，无节奏突变；镜头始终与家具正面保持垂直，完整展现绝对静止家具的侧面轮廓、线条美学与整体结构比例，平移过程中，焦点平缓顺滑地从家具整体结构，过渡到家具的隐蔽工艺细节特写，自然呈现板材拼接、封边工艺、五金光泽、面料真实编织肌理，材质纹理绝对无蜂窝状、颗粒状、马赛克式错乱噪点与畸变；家具全程绝对静止、形态恒定，背景空间与第一段完全统一、固定不变，无任何视觉割裂、无新增物体；画面无水印、无抖动、无不明杆子 / 多余结构\n" +
                "6-8 秒 镜头平缓回归与家具中心轴线垂直的正向机位，采用匀速缓拉长镜头，全程单方向、单速度、无任何启停切换；从工艺细节特写，平缓顺滑地过渡到家具完整中景，最终落幅到家具与空间融合的完整全景，全程家具主体的形态、结构、品类、朝向与初始帧、用户上传图片 100% 完全一致，无任何变形、替换、重构；背景空间全程固定不变，无任何新增物体、无结构改动；第 7.5-8 秒，镜头最终平稳定格在家具与空间融合的完整全景高级感画面，保持光影全程恒定，画面以极缓的线性速度柔和均匀变暗，最终干净落幅，无任何画面抖动、突兀转场与暗场跳变，完美契合高端商业宣传片的收尾标准";
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
                    String localUrl = downloadRemoteVideoToProfile(remoteVideoUrl);
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
}
