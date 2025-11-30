package com.xhh.yupicture.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.interfaces.dto.user.UserLoginRequest;
import com.xhh.yupicture.interfaces.dto.user.UserQueryRequest;
import com.xhh.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.xhh.yupicture.interfaces.vo.user.LoginUserVO;
import com.xhh.yupicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author 机hui难得
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-07-28 23:32:14
 */
public interface UserDomainService extends IService<User> {

    /**
     * 用户注册
     *
     * @param request 请求对象
     * @return 用户id
     */
    long userRegister(UserRegisterRequest request);

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求对象
     * @param request      http请求
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(UserLoginRequest loginRequest, HttpServletRequest request);

    /**
     * 获取登录用户
     *
     * @param request http请求
     * @return 登录用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request http请求
     */
    void logout(HttpServletRequest request);

    /**
     * 对密码进行加密
     *
     * @param password 需要加密的密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String password);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 登录用户
     * @return 脱敏后的用户信息
     */
    LoginUserVO getUserLoginVO(User user);

    /**
     * 获取脱敏后的用户信息(单个)
     *
     * @param user 登录用户
     * @return 脱敏后的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息(列表)
     *
     * @param userList 登录用户(列表)
     * @return 脱敏后的用户信息(列表)
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 将分页条件转成查询条件
     *
     * @param userQueryRequest 分页条件
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    Long addUser(User user);

    Boolean removeById(Long id);

    User getById(long id);

    Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper);

    List<User> listByIds(Set<Long> userIdSet);
}
