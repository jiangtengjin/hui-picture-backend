package com.xhh.yupicture.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicture.application.service.SpaceApplicationService;
import com.xhh.yupicture.domain.service.SpaceDomainService;
import com.xhh.yupicture.domain.service.SpaceUserDomainService;
import com.xhh.yupicture.domain.service.UserDomainService;
import com.xhh.yupicture.domain.space.entity.Space;
import com.xhh.yupicture.domain.space.entity.SpaceUser;
import com.xhh.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.xhh.yupicture.domain.space.valueobject.SpaceRoleEnum;
import com.xhh.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.infrastructure.exception.ErrorCode;
import com.xhh.yupicture.infrastructure.exception.ThrowUtils;
import com.xhh.yupicture.infrastructure.mapper.SpaceMapper;
import com.xhh.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.xhh.yupicture.interfaces.dto.space.SpaceQueryRequest;
import com.xhh.yupicture.interfaces.vo.space.SpaceVO;
import com.xhh.yupicture.interfaces.vo.user.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 机hui难得
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-08-12 22:45:48
*/
@Service
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceApplicationService {

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    @Lazy
    private SpaceUserDomainService spaceUserDomainService;

//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 添加空间
     *
     * @param spaceAddRequest   空间
     * @param loginUser         登录用户
     * @return                  创建的空间id
     */
    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 设置默认值
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充字段
        this.fillSpaceBySpaceLevel(space);
        // 1、校验参数
        space.validSpace(true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 2、权限校验
        ThrowUtils.throwIf(
                SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel()
                        && !loginUser.isAdmin(),
                ErrorCode.NOT_AUTH_ERROR,
                "无权限创建指定级别的空间"
                );
        // 3、写入数据库，为了保证每个用户只能创建一个空间，这里使用 锁 + 事务的形式
        // 针对用户加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能拥有一个空间");
                // 写入数据库
                boolean result = spaceDomainService.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                // 如果是团队空间，关联新增团队成员记录
                if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserDomainService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }
                // 创建分表
//                dynamicShardingManager.createSpacePictureTable(space);
                // 返回新写入的数据 id
                return space.getId();

            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }


    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userDomainService.getById(userId);
            UserVO userVO = userDomainService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> pictureList = spacePage.getRecords();
        Page<SpaceVO> pictureVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> pictureVOList = pictureList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userDomainService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(SpaceVO -> {
            Long userId = SpaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            SpaceVO.setUser(userDomainService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
       spaceDomainService.checkSpaceAuth(loginUser, space);
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    /**
     * 根据空间级别，自动填充限额
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }



}




