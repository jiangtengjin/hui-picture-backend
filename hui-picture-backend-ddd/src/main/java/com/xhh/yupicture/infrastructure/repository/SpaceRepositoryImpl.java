package com.xhh.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicture.domain.repository.SpaceRepository;
import com.xhh.yupicture.domain.space.entity.Space;
import com.xhh.yupicture.infrastructure.mapper.SpaceMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {
}
