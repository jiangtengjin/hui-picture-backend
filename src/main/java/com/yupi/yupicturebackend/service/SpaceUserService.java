package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import com.yupi.yupicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 机hui难得
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-08-30 19:10:58
*/
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest   空间
     * @return                      创建的空间id
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);


    /**
     * 校验空间参数
     * @param spaceUser 空间成员对象
     * @param add 是否添加时校验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取单个空间的VO对象
     * @param spaceUser space 对象
     * @param request request请求
     * @return 对应图片的VO
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员VO list
     * @param spaceUserList page对象
     * @return 分页的VO
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);


    /**
     * 获取查询条件的queryWrapper
     * @param spaceUserQueryRequest 空间请求类
     * @return 可用来查询的queryWrapper
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

}
