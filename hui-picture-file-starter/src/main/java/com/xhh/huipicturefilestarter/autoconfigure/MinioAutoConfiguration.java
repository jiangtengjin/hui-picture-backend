package com.xhh.huipicturefilestarter.autoconfigure;

import com.xhh.huipicturefilestarter.service.MinioService;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnClass(MinioClient.class)
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(prefix = "minio", value = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(MinioProperties properties) {
        log.info("Initializing MinIO client with endpoint: {}", properties.getEndpoint());
        
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MinioService minioService(MinioClient minioClient, MinioProperties properties) {
        return new MinioService(minioClient, properties);
    }
}