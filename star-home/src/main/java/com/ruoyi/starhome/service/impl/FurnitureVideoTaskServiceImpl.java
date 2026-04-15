package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.starhome.domain.FurnitureNumberApiPoolDO;
import com.ruoyi.starhome.domain.FurnitureVideoGenerationTaskDO;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageItemResp;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageResp;
import com.ruoyi.starhome.mapper.FurnitureNumberApiPoolMapper;
import com.ruoyi.starhome.mapper.FurnitureVideoGenerationTaskMapper;
import com.ruoyi.starhome.mapper.FurnitureVideoTaskMapper;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import com.ruoyi.starhome.util.StarhomeFileUrlUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FurnitureVideoTaskServiceImpl implements IFurnitureVideoTaskService {

    private static final String IMAGE2IMAGE_T8STAR_API = "image2image_t8star_api";
    private static final String VIDEO_GENERATION_STATUS_URL = "https://ai.t8star.cn/v2/videos/generations/";

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
    private FurnitureVideoTaskPostProcessService furnitureVideoTaskPostProcessService;

    @Autowired
    private StarhomeFileUrlUtils starhomeFileUrlUtils;


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
        String mergedRemoteUrl = starhomeFileUrlUtils.toPublicFileUrl(resolveProfilePathByLocalUrl(mergedLocalUrl).toFile());

        FurnitureVideoGenerationTaskDO successUpdate = new FurnitureVideoGenerationTaskDO();
        successUpdate.setId(header.getId());
        successUpdate.setLocalFinalVideoUrl(mergedLocalUrl);
        successUpdate.setRemoteFinalVideoUrl(mergedRemoteUrl);
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
            log.info("localPaths:{}", lines);
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
        FurnitureVideoTaskDO updatedTask = furnitureVideoTaskPostProcessService.updateVideoTaskByResponse(taskId, responseText);

        if (updatedTask != null) {
            try {
                furnitureVideoTaskPostProcessService.handleFailedVideoTaskIfNeeded(updatedTask);
            } catch (Exception e) {
                log.error("处理失败任务逻辑异常，不影响主任务状态提交, taskId={}", taskId, e);
            }
            try {
                furnitureVideoTaskPostProcessService.handleNextVideoSegmentIfNeeded(updatedTask);
            } catch (Exception e) {
                log.error("处理下一段任务逻辑异常，不影响主任务状态提交, taskId={}", taskId, e);
            }
        }

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
