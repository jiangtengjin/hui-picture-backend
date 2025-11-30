package com.xhh.huipicturefilestarter.service;

import com.xhh.huipicturefilestarter.autoconfigure.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    private final MinioProperties properties;

    /**
     * 初始化存储桶
     */
    public void initBucket() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getBucketName())
                .build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.getBucketName())
                    .build());
            log.info("Created bucket: {}", properties.getBucketName());
        }
    }

    /**
     * 上传普通文件
     * @param file      文件对象
     * @param fileName  文件名称
     * @return           文件访问 URL
     * @throws Exception
     */
    public String uploadFile(MultipartFile file, String fileName) throws Exception {
        initBucket();

        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
        return getFileUrl(fileName);
    }

    /**
     * 分片上传
     *
     * @param uploadId        分片 ID
     * @param chunkNumber    分片序号
     * @param inputStream    输入流
     * @param chunkSize      分片大小
     * @throws Exception
     */
    public void chunkUpload(String uploadId, int chunkNumber, InputStream inputStream, long chunkSize) throws Exception {
        initBucket();

        String chunkName = uploadId + "/chunks/" + chunkNumber;
        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(chunkName)
                        .stream(inputStream, chunkSize, -1)
                        .build());
    }

    /**
     * 合并分片
     *
     * @param uploadId           分片 ID
     * @param finalFileName      最终文件名称
     * @param totalChunks        分片总数
     * @throws Exception
     */
    public void mergeChunks(String uploadId, String finalFileName, int totalChunks) throws Exception{
        ArrayList<ComposeSource> sources = new ArrayList<>();

        for (int i = 1; i <= totalChunks; i++) {
            String chunkName = uploadId + "/chunks/" + i;
            sources.add(ComposeSource.builder()
                    .bucket(properties.getBucketName())
                    .object(chunkName)
                    .build());
        }

        // 合并文件
        minioClient.composeObject(ComposeObjectArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(finalFileName)
                        .sources(sources)
                        .build());

        // 清理分片
        for (int i = 1; i <= totalChunks; i++) {
            String chunkName = uploadId + "/chunks/" + i;
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(chunkName)
                    .build());

        }
    }

    /**
     * 获取已上传的分片
     *
     * @param uploadId       分片 ID
     * @param totalChunks    分片总数
     * @return
     * @throws Exception
     */
    public List<Integer> getUploadedChunks(String uploadId, int totalChunks) throws Exception {
        ArrayList<Integer> uploadChunks = new ArrayList<>();

        for (int i = 1; i < totalChunks; i++) {
            String chunkName = uploadId + "/chunks/" + i;
            try {
                minioClient.statObject(StatObjectArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(chunkName)
                        .build());
                uploadChunks.add(i);
            } catch (Exception e) {
                // 分片不存在
                log.error("分片 {} 不存在", i, e);
            }
        }

        return uploadChunks;
    }

    /**
     * 获取文件访问 URL
     *
     * @param fileName  文件名称
     * @return          文件访问 URL
     */
    private String getFileUrl(String fileName) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(properties.getBucketName())
                        .object(fileName)
                        .expiry(properties.getPresignedUrlExpiry(), TimeUnit.SECONDS)
                        .build());
    }

    /**
     * 下载文件
     *
     * @param fileName       文件名称
     * @return               文件字节数组
     * @throws Exception
     */
    public byte[] downloadFile(String fileName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(fileName)
                    .build()).readAllBytes();
    }

    /**
     * 删除文件
     *
     * @param fileName  文件名称
     * @throws Exception
     */
    public void deleteFile(String fileName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(properties.getBucketName())
                .object(fileName)
                .build());
    }

}
