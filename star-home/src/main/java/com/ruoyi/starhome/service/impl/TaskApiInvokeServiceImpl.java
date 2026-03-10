package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.domain.FurnitureNumberApiPoolDO;
import com.ruoyi.starhome.domain.FurnitureUserPackageRightsDO;
import com.ruoyi.starhome.domain.dto.AiApiCallResult;
import com.ruoyi.starhome.domain.dto.FileInfo;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
import com.ruoyi.starhome.mapper.FurnitureNumberApiPoolMapper;
import com.ruoyi.starhome.mapper.FurnitureUserPackageRightsMapper;
import com.ruoyi.starhome.service.ApiCallMonitorCacheService;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TaskApiInvokeServiceImpl implements ITaskApiInvokeService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();;

    @Autowired
    private FurnitureUserPackageRightsMapper furnitureUserPackageRightsMapper;

    @Autowired
    private FurnitureNumberApiPoolMapper furnitureNumberApiPoolMapper;

    @Autowired
    private ApiCallMonitorCacheService apiCallMonitorCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskApiInvokeResponse invokeTaskApi(TaskApiInvokeRequest request) {
        if (request == null || request.getUserId() == null || request.getApiNumber() == null) {
            throw new ServiceException("userId和apiNumber不能为空");
        }

//        Date now = new Date();
//        FurnitureUserPackageRightsDO rights = furnitureUserPackageRightsMapper.selectOne(
//                new LambdaQueryWrapper<FurnitureUserPackageRightsDO>()
//                        .eq(FurnitureUserPackageRightsDO::getUserId, request.getUserId())
//                        .eq(FurnitureUserPackageRightsDO::getIsActive, 1)
//                        .and(w -> w.isNull(FurnitureUserPackageRightsDO::getExpireTime)
//                                .or().ge(FurnitureUserPackageRightsDO::getExpireTime, now))
//                        .orderByDesc(FurnitureUserPackageRightsDO::getId)
//                        .last("limit 1")
//        );
//
//        if (rights == null) {
//            throw new ServiceException("用户没有可用套餐权益");
//        }
//
//        Integer remainingCalls = rights.getRemainingCalls();
//        boolean unlimited = remainingCalls != null && remainingCalls == -1;
//        if (!unlimited && (remainingCalls == null || remainingCalls <= 0)) {
//            throw new ServiceException("剩余调用次数不足");
//        }

        AiApiCallResult callResult = callAiApiByApiNumber(request);

        // 扣减次数
//        rights.setUsedCalls(rights.getUsedCalls() == null ? 1 : rights.getUsedCalls() + 1);
//        if (!unlimited) {
//            rights.setRemainingCalls(remainingCalls - 1);
//        }
//        furnitureUserPackageRightsMapper.updateById(rights);
//
//        // 调用监控先写缓存
//        apiCallMonitorCacheService.recordCall(request.getUserId());

        TaskApiInvokeResponse response = new TaskApiInvokeResponse();
        response.setUserId(request.getUserId());
        response.setApiNumber(request.getApiNumber());
        response.setCallCost(callResult.getCallCost());
        response.setApiResult(callResult.getApiResult());
//        response.setUsedCalls(rights.getUsedCalls());
//        response.setRemainingCalls(unlimited ? -1 : rights.getRemainingCalls());
        return response;
    }

    /**
     * 按 apiNumber 查询对应接口配置。
     * 说明：这里只做配置查询占位，后续真实AI调用业务由你补充。
     */
    private AiApiCallResult callAiApiByApiNumber(TaskApiInvokeRequest request) {
        String user = String.valueOf(request.getUserId());
        FurnitureNumberApiPoolDO apiPool = furnitureNumberApiPoolMapper.selectOne(
                new LambdaQueryWrapper<FurnitureNumberApiPoolDO>()
                        .eq(FurnitureNumberApiPoolDO::getNumber, String.valueOf(request.getApiNumber()))
                        .last("limit 1")
        );

        if (apiPool == null) {
            throw new ServiceException("未找到apiNumber对应的接口配置: " + request.getApiNumber());
        }

        AiApiCallResult result = new AiApiCallResult();
        result.setCallCost(BigDecimal.ZERO);
        List<String> fileIds = null;
        try {
            List<MultipartFile> files = request.getFiles();

            List<FileInfo> fileInfos = new ArrayList<>();
            for (MultipartFile file : files) {
                fileInfos.add(new FileInfo(
                        file.getBytes(),
                        file.getOriginalFilename(),
                        file.getContentType()
                ));
            }
            fileIds = uploadMultipleFiles(fileInfos,user,apiPool.getApiUrl(),apiPool.getApiKey());
        } catch (IOException e) {
            log.error("上传图片至dify失败:{}",e.getMessage());
            throw new ServiceException("上传图片至dify失败");
        }
        try {
            result.setApiResult(runWorkflowWithMultipleFiles(request.getQuestion(),fileIds,user,apiPool.getApiUrl(),apiPool.getApiKey()));
        } catch (IOException e) {
            log.error("获取工作流结果异常:{}",e.getMessage());
            throw new ServiceException("获取工作流结果异常");
        }
        return result;
    }

    /**
     * 调用工作流，传入多个文件ID
     */
    public String runWorkflowWithMultipleFiles(String question,List<String> fileIds, String user,String url,String apiKey) throws IOException {
        // 构建 files 数组
        ObjectNode inputs = mapper.createObjectNode();
        ArrayNode filesArray = mapper.createArrayNode();
        for (String fileId : fileIds) {
            ObjectNode fileObj = mapper.createObjectNode();
            fileObj.put("type", "image");            // 根据实际类型调整，如 "document", "audio" 等
            fileObj.put("transfer_method", "local_file");
            fileObj.put("upload_file_id", fileId);
            filesArray.add(fileObj);
        }

        // 将文件数组赋给自定义变量名（必须与开始节点中的变量名完全一致）
        inputs.set("fileArray", filesArray);
        inputs.put("question", question);

        ObjectNode root = mapper.createObjectNode();
        root.set("inputs", inputs);                     // 放入 inputs 对象
        root.put("user", user);
        root.put("response_mode", "blocking");

        String jsonBody = mapper.writeValueAsString(root);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
        );

        Request request = new Request.Builder()
                .url(url + "/workflows/run") // 根据应用类型调整
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("工作流调用失败: " + response.code() + " - " + response.body().string());
            }
            return response.body().string();
        }
    }

    /**
     * 上传多个文件，返回文件ID列表
     */
    public List<String> uploadMultipleFiles(List<FileInfo> files, String user, String url, String apiKey) throws IOException {
        List<String> fileIds = new ArrayList<>();
        for (FileInfo fileInfo : files) {
            String fileId = uploadSingleFile(fileInfo.bytes, fileInfo.fileName, fileInfo.mimeType, user, url, apiKey);
            fileIds.add(fileId);
        }
        return fileIds;
    }

    /**
     * 上传文件字节数组到 Dify
     * @param fileBytes 文件内容的字节数组
     * @param fileName  文件名（包括扩展名，如 "image.png"）
     * @param mimeType  MIME类型，如 "image/png"
     * @param user      用户标识
     * @return 文件ID
     */
    private String uploadSingleFile(byte[] fileBytes, String fileName, String mimeType, String user,String url,String apiKey) throws IOException {
        // 构建 multipart 请求体，直接使用字节数组
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName,
                        RequestBody.create(MediaType.parse(mimeType), fileBytes))
                .addFormDataPart("user", user)
                .build();

        Request request = new Request.Builder()
                .url(url + "/files/upload")
                .header("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("上传失败: " + response.code() + " - " + response.body().string());
            }
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);
            return root.path("id").asText();
        }
    }

}
