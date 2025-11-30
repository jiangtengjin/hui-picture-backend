package com.xhh.yupicture.domain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicture.domain.repository.UserRepository;
import com.xhh.yupicture.domain.service.UserDomainService;
import com.xhh.yupicture.domain.user.constant.UserConstant;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.domain.user.valueobject.UserRoleEnum;
import com.xhh.yupicture.infrastructure.exception.ErrorCode;
import com.xhh.yupicture.infrastructure.exception.ThrowUtils;
import com.xhh.yupicture.infrastructure.mapper.UserMapper;
import com.xhh.yupicture.interfaces.dto.user.UserLoginRequest;
import com.xhh.yupicture.interfaces.dto.user.UserQueryRequest;
import com.xhh.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.xhh.yupicture.interfaces.vo.user.LoginUserVO;
import com.xhh.yupicture.interfaces.vo.user.UserVO;
import com.xhh.yupicture.shared.auth.StpKit;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 机hui难得
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-07-28 23:32:14
 */
@Service
public class UserDomainServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserDomainService {

    @Resource
    private UserRepository userRepository;

    /**
     * 用户注册
     *
     * @param request 请求对象
     * @return 用户id
     */
    @Override
    public long userRegister(UserRegisterRequest request) {
        // 1、判断账户是否存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserAccount, request.getUserAccount());
        long res = this.count(wrapper);
        ThrowUtils.throwIf(
                res > 0,
                ErrorCode.PARAMS_ERROR,
                "账号重复");

        // 3、密码加密
        String encryptPassword = getEncryptPassword(request.getUserPassword());

        // 4、插入数据
        User user = new User();
        user.setUserAccount(request.getUserAccount());
        user.setUserPassword(encryptPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setUserName("default");
        boolean save = userRepository.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");

        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest loginRequest, HttpServletRequest request) {
        // 1、登录密码加密
        String encryptPassword = getEncryptPassword(loginRequest.getUserPassword());

        // 3、查询数据库
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserAccount, loginRequest.getUserAccount());
        wrapper.eq(User::getUserPassword, encryptPassword);
        User user = userRepository.getOne(wrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "账号或者密码错误");

        // 4、记录用户的登录状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 5、记录用户的登录态到 sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        return this.getUserLoginVO(user);
    }

    /**
     * 获取登录用户
     *
     * @param request http请求
     * @return 登录用户信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1、判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        ThrowUtils.throwIf(
                currentUser == null || currentUser.getId() == null,
                ErrorCode.NOT_LOGIN_ERROR);

        // 获取到的用户可能是老的，需要从数据库中再查一次
        currentUser = userRepository.getById(currentUser.getId());
        // 这里处理一种特殊情况，用户被删除了会导致查不到数据
        ThrowUtils.throwIf(
                currentUser == null,
                ErrorCode.NOT_LOGIN_ERROR);

        // 2、返回结果
        return currentUser;
    }

    @Override
    public void logout(HttpServletRequest request) {
        // 1、判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(
                userObj == null,
                ErrorCode.OPERATION_ERROR, "未登录");

        // 2、移除session
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
    }

    /**
     * 对密码进行加密
     *
     * @param password 需要加密的密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String password) {
        // 盐值，混淆密码
        final String SALT = "WOSNEnlkiw948q";
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 登录用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getUserLoginVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = BeanUtil.copyProperties(user, LoginUserVO.class);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(userQueryRequest), ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public Long addUser(User user) {
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = this.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userRepository.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    public Boolean removeById(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public boolean updateById(User user) {
        return userRepository.updateById(user);
    }

    @Override
    public User getById(long id) {
        return userRepository.getById(id);
    }

    @Override
    public Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper) {
        return userRepository.page(userPage, queryWrapper);
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userRepository.listByIds(userIdSet);
    }

}




