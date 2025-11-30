package com.xhh.yupicture.interfaces.controller;

import com.xhh.yupicture.application.service.SpaceAnalyzeApplicationService;
import com.xhh.yupicture.application.service.UserApplicationService;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.infrastructure.common.BaseResponse;
import com.xhh.yupicture.infrastructure.common.ResultUtils;
import com.xhh.yupicture.infrastructure.exception.ErrorCode;
import com.xhh.yupicture.infrastructure.exception.ThrowUtils;
import com.xhh.yupicture.interfaces.dto.space.analyze.*;
import com.xhh.yupicture.interfaces.vo.space.analyze.*;
import com.xhh.yupicture.shared.auth.annotation.SaSpaceCheckPermission;
import com.xhh.yupicture.shared.auth.constans.SpaceUserAuthConstant;
import com.xhh.yupicture.domain.space.entity.Space;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeApplicationService spaceAnalyzeApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 获取空间使用状态
     */
    @PostMapping("/usage")
    @SaSpaceCheckPermission(SpaceUserAuthConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeApplicationService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    // 获取空间分类分析数据
    @PostMapping("/category")
    @SaSpaceCheckPermission(SpaceUserAuthConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> resultList = spaceAnalyzeApplicationService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(resultList);
    }

    /**
     * 获取空间标签分析数据
     */
    @PostMapping("/tag")
    @SaSpaceCheckPermission(SpaceUserAuthConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> resultList = spaceAnalyzeApplicationService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(resultList);
    }

    /**
     * 获取空间图片大小分析数据
     */
    @PostMapping("/size")
    @SaSpaceCheckPermission(SpaceUserAuthConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> resultList = spaceAnalyzeApplicationService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(resultList);
    }

    /**
     * 获取空间用户上传行为分析数据
     */
    @PostMapping("/user")
    @SaSpaceCheckPermission(SpaceUserAuthConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> resultList = spaceAnalyzeApplicationService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(resultList);
    }

    /**
     * 空间使用排行分析
     */
    @PostMapping("/rank")
    @SaSpaceCheckPermission(SpaceUserAuthConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<Space> resultList = spaceAnalyzeApplicationService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(resultList);
    }
}
