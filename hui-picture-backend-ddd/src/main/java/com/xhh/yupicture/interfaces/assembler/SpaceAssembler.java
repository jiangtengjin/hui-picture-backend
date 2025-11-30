package com.xhh.yupicture.interfaces.assembler;

import com.xhh.yupicture.domain.space.entity.Space;
import com.xhh.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.xhh.yupicture.interfaces.dto.space.SpaceEditRequest;
import com.xhh.yupicture.interfaces.dto.space.SpaceUpdateRequest;
import org.springframework.beans.BeanUtils;

public class SpaceAssembler {

    public static Space toSpaceEntity(SpaceAddRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceUpdateRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceEditRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }
}
