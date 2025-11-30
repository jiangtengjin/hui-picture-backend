package com.xhh.huipicturefilestarter.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO服务地址
     */
    private String endpoint = "http://localhost:9000";

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 秘密密钥
     */
    private String secretKey;

    /**
     * 默认存储桶名称
     */
    private String bucketName = "default-bucket";

    /**
     * 是否启用分片上传功能
     */
    private boolean chunkUploadEnabled = false;

    /**
     * 分片大小（字节）
     */
    private long chunkSize = 5 * 1024 * 1024;

    /**
     * 预签名URL过期时间（秒）
     */
    private int presignedUrlExpiry = 3600;
}
