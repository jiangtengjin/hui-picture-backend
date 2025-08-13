package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;

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
     * 根据空间级别，自动填充限额
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);
}
