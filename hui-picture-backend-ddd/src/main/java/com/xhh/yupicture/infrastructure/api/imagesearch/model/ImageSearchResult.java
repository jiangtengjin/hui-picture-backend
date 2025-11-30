package com.xhh.yupicture.infrastructure.api.imagesearch.model;

import lombok.Data;

/**
 * 以图搜图接口返回结果类
 */
@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
