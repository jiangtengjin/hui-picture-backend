package com.xhh.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhh.yupicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xhh.yupicturebackend.model.dto.picture.*;
import com.xhh.yupicturebackend.model.entity.Picture;
import com.xhh.yupicturebackend.model.entity.User;
import com.xhh.yupicturebackend.model.vo.PictureVO;
import org.springframework.scheduling.annotation.Async;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 机hui难得
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-08-03 15:11:27
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    /**
     * 获取图片信息(单个)
     * @param picture  图片
     * @param request  http请求
     * @return 图片信息(附带创建图片的用户信息)
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 用于更新和修改方法中校验图片信息
     * @param picture 需要校验的图片
     */
    void validPicture(Picture picture);

    /**
     * 将分页条件转成查询条件
     *
     * @param pictureQueryRequest 分页条件
     * @return 查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填图片审核相关字段
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 删除 cos 上的图片
     * @param oldPicture 需要删除的图片
     */
    @Async
    void clearPictureFile(Picture oldPicture);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    void deletePicture(long pictureId, User loginUser);

    /**
     * 校验图片的空间权限
     * @param loginUser     登录用户
     * @param picture       图片
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     *  以颜色搜图
     * @param spaceId    空间id
     * @param color      颜色
     * @param loginUser  登录用户
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String color, User loginUser);

    /**
     * 批量修改图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
