package com.xhh.yupicture.domain.user.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xhh.yupicture.domain.user.valueobject.UserRoleEnum;
import com.xhh.yupicture.infrastructure.exception.ErrorCode;
import com.xhh.yupicture.infrastructure.exception.ThrowUtils;
import com.xhh.yupicture.interfaces.dto.user.UserLoginRequest;
import com.xhh.yupicture.interfaces.dto.user.UserRegisterRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户
 *
 * @TableName user
 */
@TableName(value = "user")
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1943409982228182696L;

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/vip
     */
    private String userRole;

    /**
     * 会员过期时间
     */
    private Date vipExpireTime;

    /**
     * 会员兑换码
     */
    private String vipCode;

    /**
     * 会员编号
     */
    private Long vipNumber;

    /**
     * 分享码
     */
    private String shareCode;

    /**
     * 邀请用户 id
     */
    private Long inviteUser;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 校验用户注册
     *
     * @param request
     */
    public static void validUserRegister(UserRegisterRequest request) {
        ThrowUtils.throwIf(StrUtil.hasBlank(
                request.getUserAccount(),
                request.getUserPassword(),
                request.getCheckPassword()), ErrorCode.PARAMS_ERROR, "参数为空");

        ThrowUtils.throwIf(
                request.getUserAccount().length() < 4,
                ErrorCode.PARAMS_ERROR,
                "用户账户过短");

        ThrowUtils.throwIf(
                request.getUserPassword().length() < 8 || request.getCheckPassword().length() < 8,
                ErrorCode.PARAMS_ERROR,
                "用户密码过短");

        ThrowUtils.throwIf(
                !request.getUserPassword().equals(request.getCheckPassword()),
                ErrorCode.PARAMS_ERROR,
                "两次输入的密码不一致");
    }

    /**
     * 校验用户登录
     *
     * @param loginRequest
     */
    public static void validUserLogin(UserLoginRequest loginRequest) {
        ThrowUtils.throwIf(StrUtil.hasBlank(
                        loginRequest.getUserAccount(),
                        loginRequest.getUserPassword()),
                ErrorCode.PARAMS_ERROR, "参数为空");

        ThrowUtils.throwIf(
                loginRequest.getUserAccount().length() < 4,
                ErrorCode.PARAMS_ERROR,
                "账号错误");

        ThrowUtils.throwIf(
                loginRequest.getUserPassword().length() < 8,
                ErrorCode.PARAMS_ERROR,
                "密码错误");
    }

    /**
     * 是否为管理员
     *
     * @return
     */
    public boolean isAdmin() {
        return UserRoleEnum.ADMIN.getValue().equals(this.getUserRole());
    }

}