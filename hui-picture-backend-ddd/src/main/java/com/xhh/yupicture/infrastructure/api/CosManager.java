package com.xhh.yupicture.infrastructure.api;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.xhh.yupicture.infrastructure.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用的对象存储操作
 */
@Component
public class CosManager {

    @Resource
    CosClientConfig cosClientConfig;

    @Resource
    COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 要上传的对象
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        return putObjectResult;
    }

    /**
     * 下载
     *
     * @param key 唯一键
     * @return 下载对象
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象(附带图片信息)
     *
     * @param key  唯一键
     * @param file 要上传的对象
     * @return 上传结果
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 对图片进行处理（获取图片基本信息）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        // 图片压缩(转换成 webp 格式)
        String webpKey = FileUtil.mainName(file) + ".webp";
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 压缩规则
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        // 缩略图处理，仅对 2kb的图片生成缩略图
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRole = new PicOperations.Rule();
            thumbnailRole.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            thumbnailRole.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(file) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRole.setFileId(thumbnailKey);
            rules.add(thumbnailRole);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        return putObjectResult;
    }


    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
