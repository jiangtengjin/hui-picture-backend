package com.xhh.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicture.infrastructure.mapper.UserMapper;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * @author 机hui难得
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-07-28 23:32:14
 */
@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User>
        implements UserRepository {
}




