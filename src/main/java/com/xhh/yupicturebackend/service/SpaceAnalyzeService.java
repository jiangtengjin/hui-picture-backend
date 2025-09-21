package com.xhh.yupicturebackend.service;

import com.xhh.yupicturebackend.model.dto.space.analyze.*;
import com.xhh.yupicturebackend.model.entity.Space;
import com.xhh.yupicturebackend.model.entity.User;
import com.xhh.yupicturebackend.model.vo.space.analyze.*;

import java.util.List;

/**
* @author 机hui难得
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-08-12 22:45:48
*/
public interface SpaceAnalyzeService {

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest SpaceUsageAnalyzeRequest 请求参数
     * @param loginUser                当前登录用户
     * @return SpaceUsageAnalyzeResponse 分析结果
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 获取空间分类使用分析数据
     *
     * @param request
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest request, User loginUser);

    /**
     * 获取空间标签分析数据
     *
     * @param request
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest request, User loginUser);

    /**
     * 获取空间图片大小分析数据
     *
     * @param request
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest request, User loginUser);

    /**
     * 获取空间用户上传行为分析数据
     *
     * @param request
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest request, User loginUser);

    /**
     * 空间使用排行分析
     *
     * @param request
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest request, User loginUser);
}
