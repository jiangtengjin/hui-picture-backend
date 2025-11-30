package com.xhh.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicture.domain.repository.SpaceUserRepository;
import com.xhh.yupicture.domain.space.entity.SpaceUser;
import com.xhh.yupicture.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}
