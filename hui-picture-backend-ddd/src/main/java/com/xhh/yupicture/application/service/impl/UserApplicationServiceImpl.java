package com.xhh.yupicture.application.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhh.yupicture.application.service.UserApplicationService;
import com.xhh.yupicture.domain.service.UserDomainService;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.infrastructure.common.DeleteRequest;
import com.xhh.yupicture.infrastructure.exception.ErrorCode;
import com.xhh.yupicture.infrastructure.exception.ThrowUtils;
import com.xhh.yupicture.interfaces.dto.user.UserLoginRequest;
import com.xhh.yupicture.interfaces.dto.user.UserQueryRequest;
import com.xhh.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.xhh.yupicture.interfaces.vo.user.LoginUserVO;
import com.xhh.yupicture.interfaces.vo.user.UserVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author 机hui难得
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-07-28 23:32:14
 */
@Service
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    /**
     * 用户注册
     *
     * @param request 请求对象
     * @return 用户id
     */
    @Override
    public long userRegister(UserRegisterRequest request) {
        // 1、参数校验
        User.validUserRegister(request);
        // 2、用户注册
        return userDomainService.userRegister(request);
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest loginRequest, HttpServletRequest request) {
        // 1、校验参数
        User.validUserLogin(loginRequest);

        // 2、用户登录
        return userDomainService.userLogin(loginRequest, request);
    }

    /**
     * 获取登录用户
     *
     * @param request http请求
     * @return 登录用户信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    @Override
    public void logout(HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isNull(request), ErrorCode.PARAMS_ERROR);
        userDomainService.logout(request);
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 登录用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getUserLoginVO(User user) {
        return userDomainService.getUserLoginVO(user);
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVO(getUserById(id));
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return userDomainService.getUserVOList(userList);
    }

    @Override
    public Long addUser(User user) {
        return userDomainService.addUser(user);
    }

    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(deleteRequest) || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        return userDomainService.removeById(deleteRequest.getId());
    }

    @Override
    public void updateUser(User user) {
        boolean result = userDomainService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<UserVO> listUserVoByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(userQueryRequest), ErrorCode.PARAMS_ERROR);
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        Page<User> page = userDomainService.page(new Page<User>(current, pageSize),
                userDomainService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize);
        List<UserVO> userVOList = userDomainService.getUserVOList(page.getRecords());
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }

    @Override
    public List<User> listByIds(Set<Long> ids) {
        return userDomainService.listByIds(ids);
    }

    /**
     * 对密码进行加密
     *
     * @param password 需要加密的密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String password) {
        return userDomainService.getEncryptPassword(password);
    }


}




