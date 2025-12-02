package com.xhh.yupicturebackend.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class PictureServiceImplTest {

    @Resource
    private PictureServiceImpl pictureService;

    @Test
    void getPictureKey() {
        String url = "https://yu-picture-1372346116.cos.ap-guangzhou.myqcloud.com/space/1/2025-12-02_tgddpgaaNPE0qAbg.jpg";
        String key = pictureService.getPictureKey(url);
        Assertions.assertNotNull(key);
    }
}