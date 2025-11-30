package com.xhh.yupicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 以颜色搜图请求参数类
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
