package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 机hui难得
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-08-12 22:45:48
*/
public interface SpaceService extends IService<Space> {

    /**
     * 添加空间
     *
     * @param spaceAddRequest   空间
     * @param loginUser         登录用户
     * @return                  创建的空间id
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);


    /**
     * 校验空间参数
     * @param space 空间对象
     * @param add 是否添加时校验
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取单个空间的VO对象
     * @param space space 对象
     * @param request request请求
     * @return 对应图片的VO
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 分页获取空间封装
     * @param spacePage page对象
     * @param request request请求
     * @return 分页的VO
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);


    /**
     *  校验空间图片权限
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);

    /**
     * 获取查询条件的queryWrapper
     * @param spaceQueryRequest 空间请求类
     * @return 可用来查询的queryWrapper
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别，自动填充限额
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);
}
