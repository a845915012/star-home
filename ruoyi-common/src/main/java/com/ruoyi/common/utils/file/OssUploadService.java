package com.ruoyi.common.utils.file;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Objects;

@Component
public class OssUploadService
{
    @Value("${aliyun.oss.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.oss.endpoint:}")
    private String endpoint;

    @Value("${aliyun.oss.bucket:}")
    private String bucket;

    @Value("${aliyun.oss.public-domain:}")
    private String publicDomain;

    @Value("${aliyun.oss.use-signed-url:true}")
    private boolean useSignedUrl;

    @Value("${aliyun.oss.signed-url-expire-seconds:604800}")
    private long signedUrlExpireSeconds;

    public OssUploadResult uploadMultipartFile(String directory, MultipartFile file) throws IOException
    {
        validateMultipartFile(file);
        String normalizedDirectory = normalizeDirectory(directory);
        String objectKey = normalizedDirectory + FileUploadUtils.uuidFilename(file);
        String contentType = file.getContentType();

        try (InputStream inputStream = file.getInputStream())
        {
            return uploadInputStream(objectKey, inputStream, file.getSize(), contentType, file.getOriginalFilename());
        }
    }

    public OssUploadResult uploadBytes(String directory, byte[] bytes, String contentType, String extension) throws IOException
    {
        if (bytes == null || bytes.length == 0)
        {
            throw new IOException("上传内容不能为空");
        }
        String normalizedDirectory = normalizeDirectory(directory);
        String objectKey = normalizedDirectory + DateUtils.datePath() + "/" + System.currentTimeMillis() + "_" + IdUtils.fastSimpleUUID()
                + "." + normalizeExtension(extension);
        return uploadInputStream(objectKey, new ByteArrayInputStream(bytes), bytes.length, contentType, null);
    }

    public OssUploadResult uploadFile(String directory, File file, String contentType) throws IOException
    {
        if (file == null || !file.exists() || !file.isFile())
        {
            throw new IOException("上传文件不存在");
        }
        String fileName = file.getName();
        String extension = FilenameUtils.getExtension(fileName);
        String normalizedDirectory = normalizeDirectory(directory);
        String objectKey = normalizedDirectory + DateUtils.datePath() + "/" + System.currentTimeMillis() + "_" + IdUtils.fastSimpleUUID()
                + "." + normalizeExtension(extension);
        try (InputStream inputStream = new FileInputStream(file))
        {
            return uploadInputStream(objectKey, inputStream, file.length(), contentType, fileName);
        }
    }

    public OssUploadResult uploadStream(String directory, InputStream inputStream, long contentLength,
                                        String contentType, String extension) throws IOException
    {
        if (inputStream == null)
        {
            throw new IOException("上传流不能为空");
        }
        String normalizedDirectory = normalizeDirectory(directory);
        String objectKey = normalizedDirectory + DateUtils.datePath() + "/" + System.currentTimeMillis() + "_" + IdUtils.fastSimpleUUID()
                + "." + normalizeExtension(extension);
        return uploadInputStream(objectKey, inputStream, contentLength, contentType, null);
    }

    private OssUploadResult uploadInputStream(String objectKey, InputStream inputStream, long contentLength,
                                              String contentType, String originalFilename) throws IOException
    {
        ensureConfigured();

        OSS ossClient = null;
        try
        {
            ossClient = new OSSClientBuilder().build(normalizeEndpoint(endpoint), accessKeyId, accessKeySecret);
            ObjectMetadata metadata = new ObjectMetadata();
            if (StringUtils.isNotEmpty(contentType))
            {
                metadata.setContentType(contentType);
            }
            if (contentLength >= 0)
            {
                metadata.setContentLength(contentLength);
            }
            ossClient.putObject(bucket, objectKey, inputStream, metadata);

            OssUploadResult result = new OssUploadResult();
            result.setObjectKey(objectKey);
            result.setOriginalFilename(originalFilename);
            result.setUrl(buildAccessibleUrl(ossClient, objectKey));
            return result;
        }
        catch (Exception e)
        {
            throw new IOException("上传文件到OSS失败: " + e.getMessage(), e);
        }
        finally
        {
            if (ossClient != null)
            {
                try
                {
                    ossClient.shutdown();
                }
                catch (Exception ignore)
                {
                }
            }
        }
    }

    private void validateMultipartFile(MultipartFile file) throws IOException
    {
        if (file == null || file.isEmpty())
        {
            throw new IOException("上传文件不能为空");
        }
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "上传文件名不能为空");
        if (originalFilename.length() > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new IOException("文件名长度超过限制");
        }
        try
        {
            FileUploadUtils.assertAllowed(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void ensureConfigured() throws IOException
    {
        if (StringUtils.isAnyBlank(bucket, endpoint, accessKeyId, accessKeySecret))
        {
            throw new IOException("阿里云OSS配置不完整");
        }
    }

    private String normalizeDirectory(String directory)
    {
        String normalized = directory == null ? "" : directory.trim().replace("\\", "/");
        while (normalized.startsWith("/"))
        {
            normalized = normalized.substring(1);
        }
        if (!normalized.isEmpty() && !normalized.endsWith("/"))
        {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private String normalizeEndpoint(String value)
    {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://"))
        {
            return normalized;
        }
        return "https://" + normalized;
    }

    private String buildAccessibleUrl(OSS ossClient, String objectKey)
    {
        if (!useSignedUrl)
        {
            return resolvePublicDomain() + "/" + objectKey;
        }
        long expireSeconds = signedUrlExpireSeconds <= 0 ? 604800L : signedUrlExpireSeconds;
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
        return ossClient.generatePresignedUrl(bucket, objectKey, expiration).toString();
    }

    private String resolvePublicDomain()
    {
        String domain = publicDomain == null ? "" : publicDomain.trim();
        if (domain.isBlank())
        {
            domain = "https://" + bucket + "." + endpoint;
        }
        else if (!domain.startsWith("http://") && !domain.startsWith("https://"))
        {
            domain = "https://" + domain;
        }
        while (domain.endsWith("/"))
        {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain;
    }

    private String normalizeExtension(String extension)
    {
        String normalized = extension == null ? "" : extension.trim();
        if (normalized.isEmpty())
        {
            return "bin";
        }
        if (normalized.startsWith("."))
        {
            normalized = normalized.substring(1);
        }
        return normalized.isEmpty() ? "bin" : normalized;
    }

    public static class OssUploadResult
    {
        private String url;
        private String objectKey;
        private String originalFilename;

        public String getUrl()
        {
            return url;
        }

        public void setUrl(String url)
        {
            this.url = url;
        }

        public String getObjectKey()
        {
            return objectKey;
        }

        public void setObjectKey(String objectKey)
        {
            this.objectKey = objectKey;
        }

        public String getOriginalFilename()
        {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename)
        {
            this.originalFilename = originalFilename;
        }
    }
}
