package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.domain.FurnitureNumberApiPoolDO;
import com.ruoyi.starhome.domain.dto.AiApiCallResult;
import com.ruoyi.starhome.domain.dto.FileInfo;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
import com.ruoyi.starhome.mapper.FurnitureNumberApiPoolMapper;
import com.ruoyi.starhome.mapper.FurnitureUserPackageRightsMapper;
import com.ruoyi.starhome.service.IApiCallMonitorCacheService;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TaskApiInvokeServiceImpl implements ITaskApiInvokeService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build();

    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    @Autowired
    private FurnitureUserPackageRightsMapper furnitureUserPackageRightsMapper;

    @Autowired
    private FurnitureNumberApiPoolMapper furnitureNumberApiPoolMapper;

    @Autowired
    private IApiCallMonitorCacheService IApiCallMonitorCacheService;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskApiInvokeResponse invokeTaskApiBlocking(TaskApiInvokeRequest request) {
        return invokeTaskApi(request);
    }

    @Override
    public SseEmitter createStream(Long userId) {
        if (userId == null) {
            throw new ServiceException("userId不能为空");
        }
        SseEmitter emitter = new SseEmitter(0L);
        emitterMap.put(userId, emitter);
        emitter.onCompletion(() -> emitterMap.remove(userId));
        emitter.onTimeout(() -> emitterMap.remove(userId));
        emitter.onError(ex -> emitterMap.remove(userId));
        return emitter;
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
            List<String> filePaths = request.getFilePaths();
            if (filePaths == null || filePaths.isEmpty()) {
                throw new ServiceException("文件地址不能为空");
            }

            List<FileInfo> fileInfos = new ArrayList<>();
            for (String filePath : filePaths) {
                File file = resolveProfileFile(filePath);
                fileInfos.add(new FileInfo(
                        readFileBytes(file),
                        file.getName(),
                        resolveMimeType(file)
                ));
            }
            fileIds = uploadMultipleFiles(fileInfos, user, apiPool.getApiUrl(), apiPool.getApiKey());
        } catch (IOException e) {
            log.error("上传图片至dify失败:{}", e.getMessage());
            throw new ServiceException("上传图片至dify失败");
        }
        try {
            boolean useSse = Boolean.TRUE.equals(request.getUseSse());
            result.setApiResult(runWorkflowWithMultipleFiles(request.getQuestion(), fileIds, user, apiPool.getApiUrl(), apiPool.getApiKey(), useSse));
        } catch (IOException e) {
            log.error("获取工作流结果异常:{}",e.getMessage());
            throw new ServiceException("获取工作流结果异常");
        }
        return result;
    }

    /**
     * 调用工作流，传入多个文件ID（由 useSse 参数决定是否走SSE）。
     * useSse = true  -> response_mode=streaming + 通过SseEmitter推送
     * useSse = false -> response_mode=blocking  + 直接返回HTTP结果
     */
    public String runWorkflowWithMultipleFiles(String question, List<String> fileIds, String user, String url, String apiKey, boolean useSse) throws IOException {
        Long userId = Long.valueOf(user);
        SseEmitter emitter = emitterMap.get(userId);

        if (useSse && emitter == null) {
            throw new ServiceException("未找到userId对应的SSE连接: " + user + "，请先调用 /starhome/task/stream 建立连接");
        }

        ObjectNode root = buildChatMessagesPayload(question, fileIds, user, useSse ? "streaming" : "blocking");
        Request request = buildChatMessagesRequest(url, apiKey, root);

        if (!useSse) {
            return executeBlocking(request);
        }
        return executeStreaming(request, emitter, userId);
    }

    private ObjectNode buildChatMessagesPayload(String question, List<String> fileIds, String user, String responseMode) {
        ObjectNode inputs = mapper.createObjectNode();
        ArrayNode filesArray = mapper.createArrayNode();
        for (String fileId : fileIds) {
            ObjectNode fileObj = mapper.createObjectNode();
            fileObj.put("type", "image");
            fileObj.put("transfer_method", "local_file");
            fileObj.put("upload_file_id", fileId);
            filesArray.add(fileObj);
        }
        // 必须与开始节点中的变量名一致
        inputs.set("imageFiles", filesArray);
        inputs.put("question", question);

        ObjectNode root = mapper.createObjectNode();
        root.set("inputs", inputs);
        root.put("user", user);
        root.put("query", question);
        root.put("conversation_id", "");
        root.put("response_mode", responseMode);
        return root;
    }

    private Request buildChatMessagesRequest(String url, String apiKey, ObjectNode payload) throws IOException {
        String jsonBody = mapper.writeValueAsString(payload);
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
        );
        return new Request.Builder()
                .url(url + "/chat-messages")
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();
    }

    private String executeBlocking(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                ResponseBody errorBody = response.body();
                throw new IOException("工作流调用失败: " + response.code() + " - " + (errorBody == null ? "" : errorBody.string()));
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("工作流调用失败: 响应体为空");
            }
            String raw = responseBody.string();
            return extractBlockingAnswer(raw);
        }
    }

    private String extractBlockingAnswer(String rawJsonOrText) {
        if (rawJsonOrText == null || rawJsonOrText.isBlank()) {
            return "";
        }
        try {
            JsonNode root = mapper.readTree(rawJsonOrText);
            JsonNode answer = root.get("answer");
            if (answer != null && !answer.isNull()) {
                return answer.asText();
            }
            JsonNode dataAnswer = root.path("data").get("answer");
            if (dataAnswer != null && !dataAnswer.isNull()) {
                return dataAnswer.asText();
            }
            JsonNode message = root.get("message");
            if (message != null && !message.isNull()) {
                return message.asText();
            }
            return rawJsonOrText;
        } catch (Exception ignore) {
            return rawJsonOrText;
        }
    }

    private String executeStreaming(Request request, SseEmitter emitter, Long userId) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                ResponseBody errorBody = response.body();
                throw new IOException("工作流调用失败: " + response.code() + " - " + (errorBody == null ? "" : errorBody.string()));
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("工作流调用失败: 响应体为空");
            }
            String result = streamWorkflowResult(responseBody, emitter);
            emitter.complete();
            return result;
        } finally {
            emitterMap.remove(userId);
        }
    }

    private String streamWorkflowResult(ResponseBody responseBody, SseEmitter emitter) throws IOException {
        StringBuilder fullResult = new StringBuilder();
        try (BufferedSource source = responseBody.source()) {
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line == null) {
                    continue;
                }
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        ObjectNode doneEvent = mapper.createObjectNode();
                        doneEvent.put("event", "done");
                        doneEvent.put("data", "[DONE]");
                        emitter.send(SseEmitter.event().data(doneEvent));
                        emitter.complete();
                        break;
                    }
                    String chunk = extractStreamingChunk(data);
                    if(chunk.isBlank()){
                        continue;
                    }
                    fullResult.append(chunk);
                    ObjectNode messageEvent = mapper.createObjectNode();
                    messageEvent.put("event", "message");
                    messageEvent.put("data", chunk);
                    emitter.send(SseEmitter.event().data(messageEvent));
                }
            }
        } catch (Exception ex) {
            emitter.completeWithError(ex);
            throw ex instanceof IOException ? (IOException) ex : new IOException(ex.getMessage(), ex);
        }
        return fullResult.toString();
    }

    private String extractStreamingChunk(String dataLine) {
        if (dataLine == null || dataLine.isBlank()) {
            return "";
        }
        try {
            JsonNode root = mapper.readTree(dataLine);
            JsonNode answer = root.get("answer");
            if (answer != null && !answer.isNull()) {
                return answer.asText();
            }
            JsonNode delta = root.get("delta");
            if (delta != null && !delta.isNull()) {
                return delta.asText();
            }
            return "";
        } catch (Exception ignore) {
            return dataLine;
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

    private File resolveProfileFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new ServiceException("文件地址不能为空");
        }
        String normalized = filePath.trim().replace("\\", "/");
        if (!normalized.startsWith("/profile/")) {
            throw new ServiceException("文件地址格式不正确: " + filePath);
        }
        String relativePath = normalized.substring("/profile".length());
        File file = new File(RuoYiConfig.getProfile(), relativePath);
        if (!file.exists() || !file.isFile()) {
            throw new ServiceException("文件不存在: " + filePath);
        }
        return file;
    }

    private byte[] readFileBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    private String resolveMimeType(File file) throws IOException {
        Path path = file.toPath();
        String mimeType = Files.probeContentType(path);
        if (mimeType != null && !mimeType.isBlank()) {
            return mimeType;
        }
        return "application/octet-stream";
    }

}
