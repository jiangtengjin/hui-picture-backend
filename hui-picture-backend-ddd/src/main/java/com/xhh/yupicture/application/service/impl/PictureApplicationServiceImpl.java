package com.xhh.yupicture.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicture.application.service.PictureApplicationService;
import com.xhh.yupicture.domain.picture.entity.Picture;
import com.xhh.yupicture.domain.service.PictureDomainService;
import com.xhh.yupicture.domain.service.UserDomainService;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.infrastructure.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xhh.yupicture.infrastructure.mapper.PictureMapper;
import com.xhh.yupicture.interfaces.dto.picture.*;
import com.xhh.yupicture.interfaces.vo.picture.PictureVO;
import com.xhh.yupicture.interfaces.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 机hui难得
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-03 15:11:27
 */
@Service
@Slf4j
public class PictureApplicationServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureApplicationService {
    @Resource
    private PictureDomainService pictureDomainService;

    @Resource
    private UserDomainService userDomainService;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        return pictureDomainService.uploadPicture(inputSource, pictureUploadRequest, loginUser);
    }

    /**
     * 获取图片信息(单个)
     * @param picture  图片
     * @param request  http请求
     * @return 图片信息(附带创建图片的用户信息)
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userDomainService.getById(userId);
            UserVO userVO = userDomainService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        return pictureDomainService.getPictureVOPage(picturePage, request);
    }

    /**
     * 用于更新和修改方法中校验图片信息
     * @param picture 需要校验的图片
     */
    @Override
    public void validPicture(Picture picture) {
        pictureDomainService.validPicture(picture);
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.getQueryWrapper(pictureQueryRequest);
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        pictureDomainService.doPictureReview(pictureReviewRequest, loginUser);
    }

    /**
     * 填图片审核相关字段
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        pictureDomainService.fillReviewParams(picture, loginUser);
    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        return pictureDomainService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        pictureDomainService.editPicture(pictureEditRequest, loginUser);
    }


    @Override
    public void deletePicture(long pictureId, User loginUser) {
        pictureDomainService.deletePicture(pictureId, loginUser);
    }


    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String color, User loginUser) {
        return pictureDomainService.searchPictureByColor(spaceId, color, loginUser);
    }

    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        pictureDomainService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
    }

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        return pictureDomainService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
    }

}




