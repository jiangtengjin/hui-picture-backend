package com.xhh.yupicturebackend.api.imagesearch;

import com.xhh.yupicturebackend.api.imagesearch.model.ImageSearchResult;
import com.xhh.yupicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.xhh.yupicturebackend.api.imagesearch.sub.GetImageListApi;
import com.xhh.yupicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://baolong-picture-1259638363.cos.ap-shanghai.myqcloud.com//public/10000000/2025-02-15_ILJxljPdt9Kv1EM1.";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}
