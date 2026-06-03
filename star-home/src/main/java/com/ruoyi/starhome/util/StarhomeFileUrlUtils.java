package com.ruoyi.starhome.util;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class StarhomeFileUrlUtils {

    @Value("${starhome.public-file-base-url}")
    private String publicFileBaseUrl;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public String toPublicFileUrl(File file) {
        if (file == null) {
            throw new ServiceException("文件不能为空");
        }
        String profileRoot = new File(RuoYiConfig.getProfile()).getAbsolutePath().replace("\\", "/");
        String absolutePath = file.getAbsolutePath().replace("\\", "/");
        if (!absolutePath.startsWith(profileRoot)) {
            throw new ServiceException("文件路径不在profile目录下: " + absolutePath);
        }
        String relative = absolutePath.substring(profileRoot.length());
        if (!relative.startsWith("/")) {
            relative = "/" + relative;
        }
        return publicFileBaseUrl + relative;
    }

    public String downloadRemoteVideoToProfile(String remoteVideoUrl) {
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
