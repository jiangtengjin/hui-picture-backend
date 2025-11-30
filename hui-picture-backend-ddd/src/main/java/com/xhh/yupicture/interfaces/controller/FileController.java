package com.xhh.yupicture.interfaces.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.xhh.yupicture.infrastructure.annotation.AuthCheck;
import com.xhh.yupicture.infrastructure.common.BaseResponse;
import com.xhh.yupicture.infrastructure.common.ResultUtils;
import com.xhh.yupicture.domain.user.constant.UserConstant;
import com.xhh.yupicture.infrastructure.exception.BusinessException;
import com.xhh.yupicture.infrastructure.exception.ErrorCode;
import com.xhh.yupicture.infrastructure.api.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("file")
@Slf4j
public class FileController {

    @Resource
    CosManager cosManager;

    @PostMapping("test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", filename);

        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filePath, file);
            // 返回可访问的地址
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file putObject fail, filepath: {}", filePath, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败");
        } finally {
            // 删除临时文件
            if (file != null) {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.error("file delete fail, filePath: {}", filePath);
                }
            }
        }
    }


    @GetMapping("test/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void testDownloadFile(String filePath, HttpServletResponse response) throws IOException {
        COSObjectInputStream inputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filePath);
            inputStream = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(inputStream);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filePath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file getObject fail, filePath: {}", filePath, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件下载失败");
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }


}
