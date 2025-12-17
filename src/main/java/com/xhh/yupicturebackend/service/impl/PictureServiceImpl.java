package com.xhh.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicturebackend.api.aliyun.AliYunApi;
import com.xhh.yupicturebackend.api.aliyun.model.CreateOutPaintingTaskRequest;
import com.xhh.yupicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xhh.yupicturebackend.exception.BusinessException;
import com.xhh.yupicturebackend.exception.ErrorCode;
import com.xhh.yupicturebackend.exception.ThrowUtils;
import com.xhh.yupicturebackend.manager.CosManager;
import com.xhh.yupicturebackend.manager.upload.FilePictureUpload;
import com.xhh.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.xhh.yupicturebackend.manager.upload.UrlPictureUpload;
import com.xhh.yupicturebackend.mapper.PictureMapper;
import com.xhh.yupicturebackend.model.dto.file.UploadPictureResult;
import com.xhh.yupicturebackend.model.dto.picture.*;
import com.xhh.yupicturebackend.model.entity.Picture;
import com.xhh.yupicturebackend.model.entity.Space;
import com.xhh.yupicturebackend.model.entity.User;
import com.xhh.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.xhh.yupicturebackend.model.vo.PictureVO;
import com.xhh.yupicturebackend.model.vo.UserVO;
import com.xhh.yupicturebackend.service.ExpandPictureTaskService;
import com.xhh.yupicturebackend.service.PictureService;
import com.xhh.yupicturebackend.service.SpaceService;
import com.xhh.yupicturebackend.service.UserService;
import com.xhh.yupicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author 机hui难得
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-03 15:11:27
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private ThreadPoolExecutor customExecutor;

    @Resource
    private ExpandPictureTaskService expandPictureTaskService;

    @Resource
    CosManager cosManager;

    @Resource
    AliYunApi aliYunApi;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        // 判断是新增还是更新
        Long picId = null;
        if (pictureUploadRequest.getId() != null) {
            picId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        Picture oldPicture = null;
        if (picId != null) {
            oldPicture = this.getById(picId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 没传 spaceId 则复用原有图片的 spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId 必须和原有图片一致
                ThrowUtils.throwIf(
                        ObjUtil.notEqual(spaceId, oldPicture.getSpaceId()),
                        ErrorCode.PARAMS_ERROR,
                        "空间 id 不一致");
            }
        }

        // 上传图片，得到图片信息
        // 按照用户id划分目录 => 按照空间id划分
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造需要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        // 缩略图地址
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 原图地址
        picture.setOriginUrl(uploadPictureResult.getOriginUrl());
        String picName = uploadPictureResult.getPicName();
        if (ObjUtil.isNotEmpty(pictureUploadRequest) && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充设置 spaceId
        picture.setSpaceId(spaceId);
        // 图片主色调
        picture.setPicColor(uploadPictureResult.getPicColor());
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 如果picId不为空，表示更新，否则就是新增
        if (picId != null) {
            picture.setId(picId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        // 当更新操作时，需要清理图片
        if (oldPicture != null) {
            this.deletePicture(oldPicture.getId(), loginUser);
        }
        return PictureVO.objToVo(picture);
    }

    /**
     * 获取图片信息(单个)
     * @param picture  图片
     * @param request  http请求
     * @return 图片信息(附带创建图片的用户信息)
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 用于更新和修改方法中校验图片信息
     * @param picture 需要校验的图片
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 图片审核相关字段
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        // 空间相关字段
        Long spaceId = pictureQueryRequest.getSpaceId();
        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        // 编辑时间相关字段
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        // 图片审核信息
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        // 编辑时间范围查询，大于等于开始时间，小于结束时间
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // 图片审核状态
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        // 审核人id
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // 空间
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");

        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填图片审核相关字段
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 1、校验参数
        // 图片名称前缀为空时，默认等于搜索关键字
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多30条");
        // 2、获取页面
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        ThrowUtils.throwIf(ObjUtil.isNull(div), ErrorCode.OPERATION_ERROR, "获取元素失败");
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element element : imgElementList) {
            // 3、获取图片url
            String fileUrl = element.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 4、上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            try {
                if (StrUtil.isNotBlank(namePrefix)) {
                    // 设置图片名称，序号连续递增
                    pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
                }
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 删除 cos 上的图片
     * @param oldPicture 需要删除的图片
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        cosManager.deleteObject(this.getPictureKey(oldPicture.getUrl()));
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(this.getPictureKey(thumbnailUrl));
        }
        // 清理原图
        String originUrl = oldPicture.getOriginUrl();
        if (StrUtil.isNotBlank(originUrl)) {
            cosManager.deleteObject(this.getPictureKey(originUrl));
        }
    }

    @Value("${cos.client.host}")
    private String host;

    /**
     * 获取图片的 key
     *
     * @param url 图片的 url
     */
    public String getPictureKey(String url){
        return url.substring(host.length());
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
//        checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 释放额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);

    }


    /**
     * 校验图片的空间权限
     * @param loginUser     登录用户
     * @param picture       图片
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String color, User loginUser) {
        // 1、 校验参数
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceId) || StrUtil.isEmpty(color), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isNull(loginUser), ErrorCode.NOT_AUTH_ERROR);
        // 2、校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NOT_AUTH_ERROR, "没有空间访问权限");
        // 3、查询图片，过滤没有主色调的图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 没有图片，返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 将目标颜色转换为 color 对象
        Color targetColor = Color.decode(color);
        // 4、计算颜色相似度并排序
        List<PictureVO> result = pictureList.stream().sorted(Comparator.comparing(picture -> {
                    // 获取图片主色调
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片放在最后
                    if (StrUtil.isEmpty(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 计算相似度，计算出来的结果越大表示越相似，这里需要取个反
                    return -ColorSimilarUtils.calculateSimilarity(pictureColor, targetColor);
                })).map(PictureVO::objToVo)
                .limit(12) // 取12条
                .collect(Collectors.toList());
        // 5、返回结果
        return result;
    }

    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();

        // 1、校验参数
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        // 2、校验权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NOT_AUTH_ERROR,
                "没有空间访问权限");
        // 3、查询指定图片，仅选择需要的字段
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList.isEmpty()) {
            return;
        }
        // 4、更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRole(pictureList, nameRule);
        // 5、批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "数据库操作失败");
    }

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 图片大小
        Long picSize = picture.getPicSize();
        ThrowUtils.throwIf(picSize == null || picSize > 1024 * 1024 * 10, ErrorCode.PARAMS_ERROR,
                "图片大小必须小于10M");
        // 图片单边长度
        Integer picWidth = picture.getPicWidth();
        Integer picHeight = picture.getPicHeight();
        ThrowUtils.throwIf(picWidth == null || picWidth < 512 || picWidth > 4096, ErrorCode.PARAMS_ERROR,
                "图像单边范围必须在 [512, 4096]");
        ThrowUtils.throwIf(picHeight == null || picHeight < 512 || picHeight > 4096, ErrorCode.PARAMS_ERROR,
                "图像单边范围必须在 [512, 4096]");
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        taskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        // 创建任务
        CreateOutPaintingTaskResponse task = aliYunApi.createTask(taskRequest);
        Boolean result = expandPictureTaskService.createTask(task, loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "创建任务失败");
        return task;
    }

    /**
     *  nameRole 格式：图片{序号}
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRole(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        int count = 1;
        try {
            for (Picture picture : pictureList) {
                picture.setName(nameRule.replaceAll("\\{序号}", String.valueOf(count++)));
            }
        } catch (Exception e) {
            log.error("名称解析失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析失败");
        }
    }

    /**
     * 批量编辑图片分类和标签
     *
     * @param pictureEditByBatchRequest
     * @param spaceId
     * @param loginUser
     */
    public void batchDeletePicture(
            PictureEditByBatchRequest pictureEditByBatchRequest,
            Long spaceId,
            User loginUser) {
        // 参数校验
        validateBatchEditRequest(pictureEditByBatchRequest, spaceId, loginUser);
        // 查询空间下的图片
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureEditByBatchRequest.getPictureIdList())
                .list();
        if (pictureList.isEmpty()) {
            ThrowUtils.throwIf(CollUtil.isEmpty(pictureList), ErrorCode.NOT_FOUND_ERROR, "指定的图片不存在或不属于该空间");
        }
        // 分批处理避免长事务
        int batchSize = 100;
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < pictureList.size(); i += batchSize) {
            List<Picture> batch = pictureList.subList(i, Math.min(i + batchSize, pictureList.size()));
            // 异步处理每批数据
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                batch.forEach(picture -> {
                    if (StrUtil.isNotBlank(pictureEditByBatchRequest.getCategory())) {
                        picture.setCategory(pictureEditByBatchRequest.getCategory());
                    }
                    if (CollUtil.isNotEmpty(pictureEditByBatchRequest.getTags())) {
                        picture.setTags(JSONUtil.toJsonStr(pictureEditByBatchRequest.getTags()));
                    }
                });
                boolean result = this.updateBatchById(pictureList);
                ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "数据库操作失败");
            }, customExecutor);
            futures.add(future);
        }
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void validateBatchEditRequest(PictureEditByBatchRequest pictureEditByBatchRequest, Long spaceId, User loginUser) {
        ThrowUtils.throwIf(ObjUtil.isNull(pictureEditByBatchRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureEditByBatchRequest.getPictureIdList()), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        // 校验权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(loginUser.getId().equals(space.getUserId()), ErrorCode.NOT_AUTH_ERROR,
                "没有空间访问权限");
    }


}




