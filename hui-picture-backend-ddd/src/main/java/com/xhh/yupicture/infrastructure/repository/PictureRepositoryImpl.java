package com.xhh.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicture.domain.picture.entity.Picture;
import com.xhh.yupicture.domain.repository.PictureRepository;
import com.xhh.yupicture.infrastructure.mapper.PictureMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author 机hui难得
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-03 15:11:27
 */
@Service
@Slf4j
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureRepository {

}




