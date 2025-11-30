package com.xhh.yupicture.infrastructure.aop;

import com.xhh.yupicture.domain.service.UserDomainService;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.domain.user.valueobject.UserRoleEnum;
import com.xhh.yupicture.infrastructure.annotation.AuthCheck;
import com.xhh.yupicture.infrastructure.exception.ErrorCode;
import com.xhh.yupicture.infrastructure.exception.ThrowUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserDomainService userDomainService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切点
     * @param authCheck 权限校验注解
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User loginUser = userDomainService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，放行
        if (mustRole == null) {
            return joinPoint.proceed();
        }
        // 以下为，必须有该权限才能通过
        // 获取当前用户具有的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限，拒绝
        ThrowUtils.throwIf(userRoleEnum == null, ErrorCode.NOT_AUTH_ERROR);
        // 要求有管理员权限，但是没有，拒绝
        ThrowUtils.throwIf(
                UserRoleEnum.ADMIN.equals(mustRoleEnum)
                        && !UserRoleEnum.ADMIN.equals(userRoleEnum),
                ErrorCode.NOT_AUTH_ERROR);
        // 通过校验，放行
        return joinPoint.proceed();
    }

}
