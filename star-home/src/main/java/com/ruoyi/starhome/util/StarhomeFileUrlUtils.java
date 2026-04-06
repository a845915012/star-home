package com.ruoyi.starhome.util;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class StarhomeFileUrlUtils {

    @Value("${starhome.public-file-base-url}")
    private String publicFileBaseUrl;

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
}
