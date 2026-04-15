package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.security.util.SecurityFrameworkUtils;
import com.ruoyi.starhome.domain.*;
import com.ruoyi.starhome.domain.dto.AiApiCallResult;
import com.ruoyi.starhome.domain.dto.FileInfo;
import com.ruoyi.starhome.domain.dto.ImageGenerateVideoRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
import com.ruoyi.starhome.mapper.*;
import com.ruoyi.starhome.service.IApiCallMonitorCacheService;
import com.ruoyi.starhome.service.IFurnitureUserBalanceAccountService;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import com.ruoyi.starhome.util.StarhomeFileUrlUtils;
import com.ruoyi.framework.manager.AsyncManager;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;
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

    private static final String IMAGE2VIDEO_API_NUMBER = "image2video_t8star_api";

    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    @Autowired
    private FurnitureUserPackageRightsMapper furnitureUserPackageRightsMapper;

    @Autowired
    private FurnitureNumberApiPoolMapper furnitureNumberApiPoolMapper;

    @Autowired
    private IFurnitureUserBalanceAccountService furnitureUserBalanceAccountService;

    @Autowired
    private FurnitureAiCallRecordsMapper furnitureAiCallRecordsMapper;

    @Autowired
    private FurnitureVideoTaskMapper furnitureVideoTaskMapper;

    @Autowired
    private IApiCallMonitorCacheService apiCallMonitorCacheService;

    @Autowired
    private FurnitureVideoGenerationTaskMapper furnitureVideoGenerationTaskMapper;

    @Autowired
    private StarhomeFileUrlUtils starhomeFileUrlUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskApiInvokeResponse invokeTaskApi(TaskApiInvokeRequest request) {
        if (request == null || request.getUserId() == null || request.getApiNumber() == null) {
            throw new ServiceException("userId和apiNumber不能为空");
        }

        validateBalanceEnough(request.getUserId(),request.getConsumeConstants().getPrice());

        AiApiCallResult callResult = callAiApiByApiNumber(request);

        // 业务完成后扣减余额
        furnitureUserBalanceAccountService.consume(request.getUserId(), request.getConsumeConstants().getPrice());

        // 调用监控先写缓存
        apiCallMonitorCacheService.recordCall(request.getUserId());

        TaskApiInvokeResponse response = new TaskApiInvokeResponse();
        response.setUserId(request.getUserId());
        response.setApiNumber(request.getApiNumber());
        response.setCallCost(callResult.getCallCost());
        response.setApiResult(callResult.getApiResult());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskApiInvokeResponse invokeTaskApiBlocking(TaskApiInvokeRequest request) {
        return invokeTaskApi(request);
    }

    /**
     * Gemini 图生图调用（inline_data方式），入参使用 filePaths + question。
     * 功能与 invokeTaskApi 保持一致：校验余额、调用AI、扣减余额、记录调用监控。
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskApiInvokeResponse invokeGeminiImageApi(TaskApiInvokeRequest request) throws IOException {
        if (request == null || request.getUserId() == null || request.getApiNumber() == null) {
            throw new ServiceException("userId和apiNumber不能为空");
        }
        if (request.getFilePaths() == null || request.getFilePaths().isEmpty()) {
            throw new ServiceException("文件地址不能为空");
        }

        validateBalanceEnough(request.getUserId(),request.getConsumeConstants().getPrice());

        AiApiCallResult callResult = callGeminiImageApiByApiNumber(request);

        furnitureUserBalanceAccountService.consume(request.getUserId(), request.getConsumeConstants().getPrice());
        apiCallMonitorCacheService.recordCall(request.getUserId());

        TaskApiInvokeResponse response = new TaskApiInvokeResponse();
        response.setUserId(request.getUserId());
        response.setApiNumber(request.getApiNumber());
        response.setCallCost(callResult.getCallCost());
        response.setApiResult(callResult.getApiResult());
        return response;
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

    @Override
    @Transactional
    public TaskApiInvokeResponse imageGenerateVideo(ImageGenerateVideoRequest request) throws IOException {
        if (request == null || request.getImageUrls() == null || request.getImageUrls().isEmpty()) {
            throw new ServiceException("图片URL列表不能为空");
        }

        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            userId = request.getUserId();
        }

        Long generationTaskId = request.getGenerationTaskId();
        if (userId == null) {
            throw new ServiceException("用户未登录");
        }

        // 先校验余额，扣费延后到任务成功后统一处理
        validateBalanceEnough(userId,request.getConsumeConstants().getPrice());

        String apiNumber = request.getApiNumber();
        if (apiNumber == null || apiNumber.isBlank()) {
            apiNumber = IMAGE2VIDEO_API_NUMBER;
        }
        if (!IMAGE2VIDEO_API_NUMBER.equals(apiNumber)) {
            throw new ServiceException("apiNumber不正确，仅支持: " + IMAGE2VIDEO_API_NUMBER);
        }

        FurnitureNumberApiPoolDO apiPool = furnitureNumberApiPoolMapper.selectOne(
                new LambdaQueryWrapper<FurnitureNumberApiPoolDO>()
                        .eq(FurnitureNumberApiPoolDO::getNumber, apiNumber)
                        .last("limit 1")
        );
        if (apiPool == null || apiPool.getApiUrl() == null || apiPool.getApiUrl().isBlank()
                || apiPool.getApiKey() == null || apiPool.getApiKey().isBlank()) {
            throw new ServiceException("未找到图像生成视频接口配置");
        }

        List<String> publicImageUrls = new ArrayList<>();
        for (String imageUrl : request.getImageUrls()) {
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }
            publicImageUrls.add(toPublicFileUrl(imageUrl));
        }
        if (publicImageUrls.isEmpty()) {
            throw new ServiceException("图片URL列表不能为空");
        }

        try {
            String rawResponse = callImageToVideoApi(apiPool.getApiUrl(), apiPool.getApiKey(), request.getPrompt(), publicImageUrls);

            JsonNode resultNode = mapper.readTree(rawResponse);
            String taskId = getText(resultNode, "task_id");
            if (taskId == null || taskId.isBlank()) {
                taskId = getText(resultNode, "id");
            }
            FurnitureVideoGenerationTaskDO generationTaskDO;
            if (generationTaskId == null) {
                generationTaskDO = new FurnitureVideoGenerationTaskDO();
                generationTaskDO.setUserId(userId);
                generationTaskDO.setProduct(request.getProduct());
                generationTaskDO.setMaterial(request.getMaterial());
                generationTaskDO.setImageUrl(publicImageUrls.toString());
                generationTaskDO.setExpectedTaskCount(2);
                generationTaskDO.setCurrentTaskCount(0);
                generationTaskDO.setStatus("process");
                generationTaskDO.setCreateTime(LocalDateTime.now());
                furnitureVideoGenerationTaskMapper.insert(generationTaskDO);
            } else {
                generationTaskDO = furnitureVideoGenerationTaskMapper.selectById(generationTaskId);
                if (generationTaskDO == null) {
                    throw new ServiceException("未找到视频生成头任务: " + generationTaskId);
                }
                FurnitureVideoGenerationTaskDO updateHeader = new FurnitureVideoGenerationTaskDO();
                updateHeader.setId(generationTaskId);
                updateHeader.setStatus("process");
                if ((generationTaskDO.getProduct() == null || generationTaskDO.getProduct().isBlank()) && request.getProduct() != null && !request.getProduct().isBlank()) {
                    updateHeader.setProduct(request.getProduct());
                    generationTaskDO.setProduct(request.getProduct());
                }
                if ((generationTaskDO.getMaterial() == null || generationTaskDO.getMaterial().isBlank()) && request.getMaterial() != null && !request.getMaterial().isBlank()) {
                    updateHeader.setMaterial(request.getMaterial());
                    generationTaskDO.setMaterial(request.getMaterial());
                }
                furnitureVideoGenerationTaskMapper.updateById(updateHeader);
            }

            FurnitureVideoTaskDO videoTask = new FurnitureVideoTaskDO();
            videoTask.setGenerationTaskId(generationTaskDO.getId());
            videoTask.setUserId(userId);
            videoTask.setTaskId(taskId);
            videoTask.setModel(getText(resultNode, "model"));
            videoTask.setProgress(getText(resultNode, "progress"));
            videoTask.setStatus(getText(resultNode, "status"));
            videoTask.setCost(BigDecimal.ZERO);
            videoTask.setSize(getText(resultNode, "size"));
            videoTask.setSeconds(getInteger(resultNode, "seconds"));
            videoTask.setPrompt(request.getPrompt());
            videoTask.setImageUrl(String.join(",", publicImageUrls));
            videoTask.setIsComplete(isCompletedStatus(videoTask.getStatus()) ? 1 : 0);
            videoTask.setStartTime(new Date());
            furnitureVideoTaskMapper.insert(videoTask);

            // 调用监控先写缓存，扣费放到后续任务成功后处理
            apiCallMonitorCacheService.recordCall(userId);

            TaskApiInvokeResponse response = new TaskApiInvokeResponse();
            response.setUserId(videoTask.getUserId());
            response.setApiNumber(apiNumber);
            response.setCallCost(request.getConsumeConstants().getPrice());
            response.setApiResult(rawResponse);
            return response;
        } catch (IOException e) {
            if (generationTaskId != null) {
                markImageToVideoGenerationFailed(generationTaskId, userId, request, publicImageUrls, e.getMessage());
            }
            throw e;
        }
    }

    private String callImageToVideoApi(String apiUrl, String apiKey, String prompt, List<String> publicImageUrls) throws IOException {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("prompt", prompt == null ? "" : prompt);
        payload.put("model", "veo3.1-pro");
        payload.put("enhance_prompt", true);

        ArrayNode images = mapper.createArrayNode();
        for (String imageUrl : publicImageUrls) {
            if (imageUrl != null && !imageUrl.isBlank()) {
                images.add(imageUrl);
            }
        }
        payload.set("images", images);
        payload.put("aspect_ratio", "9:16");

        String endpoint = trimEndSlash(apiUrl) + "/v2/videos/generations";
        String jsonBody = mapper.writeValueAsString(payload);
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
        );

        Request request = new Request.Builder()
                .url(endpoint)
                .method("POST", body)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        List<String> errors = new ArrayList<>();
        for (int attempt = 1; attempt <= 3; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    ResponseBody errorBody = response.body();
                    throw new IOException("图生视频调用失败: " + response.code() + " - " + (errorBody == null ? "" : errorBody.string()));
                }
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new IOException("图生视频调用失败: 响应体为空");
                }
                return responseBody.string();
            } catch (IOException e) {
                String errorMessage = "第" + attempt + "次调用失败: " + e.getMessage();
                errors.add(errorMessage);
                if (attempt == 3) {
                    throw new IOException("图生视频调用失败，三次重试均失败：" + String.join("；", errors), e);
                }
            }
        }
        throw new IOException("图生视频调用失败，三次重试均失败：" + String.join("；", errors));
    }

    private void markImageToVideoGenerationFailed(Long generationTaskId, Long userId, ImageGenerateVideoRequest request,
                                                  List<String> publicImageUrls, String failReason) {
        FurnitureVideoGenerationTaskDO generationTaskDO = furnitureVideoGenerationTaskMapper.selectById(generationTaskId);
        if (generationTaskDO != null) {
            FurnitureVideoGenerationTaskDO updateHeader = new FurnitureVideoGenerationTaskDO();
            updateHeader.setId(generationTaskId);
            updateHeader.setStatus("FAIL");
            updateHeader.setErrorMessage(failReason);
            furnitureVideoGenerationTaskMapper.updateById(updateHeader);
        }

        FurnitureVideoTaskDO failTask = new FurnitureVideoTaskDO();
        failTask.setGenerationTaskId(generationTaskId);
        failTask.setUserId(userId);
        failTask.setProgress("0%");
        failTask.setStatus("FAIL");
        failTask.setCost(BigDecimal.ZERO);
        failTask.setFailReason(failReason);
        failTask.setPrompt(request == null ? null : request.getPrompt());
        failTask.setImageUrl(publicImageUrls == null ? null : String.join(",", publicImageUrls));
        failTask.setIsComplete(0);
        failTask.setProcessing(0);
        failTask.setStartTime(new Date());
        failTask.setFinishTime(new Date());
        furnitureVideoTaskMapper.insert(failTask);
    }

    private boolean isCompletedStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String lower = status.toLowerCase();
        return "completed".equals(lower) || "succeeded".equals(lower) || "success".equals(lower);
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Integer getInteger(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        String text = value.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AiApiCallResult callGeminiImageApiByApiNumber(TaskApiInvokeRequest request) throws IOException {
        FurnitureNumberApiPoolDO apiPool = furnitureNumberApiPoolMapper.selectOne(
                new LambdaQueryWrapper<FurnitureNumberApiPoolDO>()
                        .eq(FurnitureNumberApiPoolDO::getNumber, String.valueOf(request.getApiNumber()))
                        .last("limit 1")
        );

        if (apiPool == null) {
            throw new ServiceException("未找到apiNumber对应的接口配置: " + request.getApiNumber());
        }

        List<String> filePaths = request.getFilePaths();
        if (filePaths == null || filePaths.isEmpty()) {
            throw new ServiceException("文件地址不能为空");
        }

        AiApiCallResult result = new AiApiCallResult();
        result.setCallCost(BigDecimal.ZERO);
        try{
            String apiResult = generateByChatCompletions("gemini-3-pro-image-preview",request.getQuestion(), filePaths, apiPool.getApiUrl(), apiPool.getApiKey(), request.getModule(), request.getUserId());
            result.setApiResult(apiResult);
        }catch (IOException e){
            throw new IOException("图生图" + e.getMessage());
        }
        return result;
    }

    private String generateByChatCompletions(String model,String question, List<String> filePaths, String url, String apiKey, String module, Long userId) throws IOException {
        ObjectNode payload = buildChatCompletionsImagePayload(model,question, filePaths);
        Request request = buildChatCompletionsImageRequest(url, apiKey, payload);
        return executeChatCompletionsImageBlocking(request, userId, module);
    }

    private ObjectNode buildChatCompletionsImagePayload(String model,String question, List<String> filePaths) {
        ObjectNode root = mapper.createObjectNode();
        root.put("stream", false);
        root.put("model", model);

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");

        ArrayNode content = mapper.createArrayNode();
        ObjectNode textPart = mapper.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", question == null ? "" : question);
        content.add(textPart);

        for (String filePath : filePaths) {
            ObjectNode imageUrl = mapper.createObjectNode();
            imageUrl.put("url", toPublicFileUrl(filePath));
            log.info("imageUrl:{}", imageUrl.get("url").asText());
            ObjectNode imagePart = mapper.createObjectNode();
            imagePart.put("type", "image_url");
            imagePart.set("image_url", imageUrl);
            content.add(imagePart);
        }

        userMessage.set("content", content);
        messages.add(userMessage);
        root.set("messages", messages);
        return root;
    }

    private Request buildChatCompletionsImageRequest(String url, String apiKey, ObjectNode payload) throws IOException {
        String jsonBody = mapper.writeValueAsString(payload);
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
        );
        String endpoint = trimEndSlash(url) + "/v1/chat/completions";
        return new Request.Builder()
                .url(endpoint)
                .method("POST", body)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();
    }

    private String executeChatCompletionsImageBlocking(Request request, Long userId, String module) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                ResponseBody errorBody = response.body();
                throw new IOException("调用失败: " + response.code() + " - " + (errorBody == null ? "" : errorBody.string()));
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("调用失败: 响应体为空");
            }
            String raw = responseBody.string();
            log.info("raw:{}", raw);
            JsonNode root = mapper.readTree(raw);

            JsonNode usageNode = toUsageNodeFromChatCompletions(root.path("usage"));
            String model = root.path("model").asText("gemini-3-pro-image-preview");
            recordUsageAsync(userId, module, model, usageNode);

            return extractChatCompletionsImageResult(root, raw);
        }
    }

    private JsonNode toUsageNodeFromChatCompletions(JsonNode usage) {
        ObjectNode usageNode = mapper.createObjectNode();
        usageNode.put("prompt_tokens", usage.path("prompt_tokens").asText("0"));
        usageNode.put("completion_tokens", usage.path("completion_tokens").asText("0"));
        usageNode.put("total_tokens", usage.path("total_tokens").asText("0"));
        // 按新规则固定金额
        usageNode.put("total_price", "0.2025");
        return usageNode;
    }

    private String extractChatCompletionsImageResult(JsonNode root, String raw) {
        JsonNode choicesNode = root.path("choices");
        if (!choicesNode.isArray() || choicesNode.isEmpty()) {
            return raw;
        }

        JsonNode contentNode = choicesNode.path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            String content = contentNode.asText("");
            String imageUrl = extractMarkdownImageUrl(content);
            return imageUrl != null ? imageUrl : content;
        }

        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : contentNode) {
                if ("text".equals(item.path("type").asText())) {
                    sb.append(item.path("text").asText(""));
                }
            }
            if (!sb.isEmpty()) {
                String content = sb.toString();
                String imageUrl = extractMarkdownImageUrl(content);
                return imageUrl != null ? imageUrl : content;
            }
        }

        return raw;
    }

    private String extractMarkdownImageUrl(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        int prefix = text.indexOf("![");
        if (prefix < 0) {
            return null;
        }
        int leftParen = text.indexOf('(', prefix);
        int rightParen = text.indexOf(')', leftParen + 1);
        if (leftParen < 0 || rightParen < 0 || rightParen <= leftParen + 1) {
            return null;
        }
        return text.substring(leftParen + 1, rightParen).trim();
    }

    private String toPublicFileUrl(String filePath) {
        File file = resolveProfileFile(filePath);
        return starhomeFileUrlUtils.toPublicFileUrl(file);
    }

    private String trimEndSlash(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String resolveImageExtByMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "png";
        }
        String normalized = mimeType.toLowerCase();
        if (normalized.contains("jpeg") || normalized.contains("jpg")) {
            return "jpg";
        }
        if (normalized.contains("webp")) {
            return "webp";
        }
        if (normalized.contains("gif")) {
            return "gif";
        }
        return "png";
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
            result.setApiResult(runWorkflowWithMultipleFiles(request.getQuestion(), fileIds, user, apiPool.getApiUrl(), apiPool.getApiKey(), useSse,request.getModule()));
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
    public String runWorkflowWithMultipleFiles(String question, List<String> fileIds, String user, String url, String apiKey, boolean useSse, String module) throws IOException {
        Long userId = Long.valueOf(user);
        SseEmitter emitter = emitterMap.get(userId);

        if (useSse && emitter == null) {
            throw new ServiceException("未找到userId对应的SSE连接: " + user + "，请先调用 /starhome/task/stream 建立连接");
        }

        ObjectNode root = buildChatMessagesPayload(question, fileIds, user, useSse ? "streaming" : "blocking");
        Request request = buildChatMessagesRequest(url, apiKey, root);

        if (!useSse) {
            return executeBlocking(request, userId, module);
        }
        return executeStreaming(request, emitter, userId, module);
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

    private String executeBlocking(Request request,Long userId, String module) throws IOException {
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
            JsonNode root = parseStreamingJson(raw);
            if (root != null) {
                String aiMode = root.path("data").path("process_data").path("model_name").asText(null);
                JsonNode usageNode = extractUsageNode(root);
                if (usageNode != null) {
                    recordUsageAsync(userId, module, aiMode, usageNode);
                }
            }
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

    private String executeStreaming(Request request, SseEmitter emitter, Long userId, String module) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                ResponseBody errorBody = response.body();
                throw new IOException("工作流调用失败: " + response.code() + " - " + (errorBody == null ? "" : errorBody.string()));
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("工作流调用失败: 响应体为空");
            }
            String result = streamWorkflowResult(responseBody, emitter, userId, module);
            emitter.complete();
            return result;
        } finally {
            emitterMap.remove(userId);
        }
    }

    private String streamWorkflowResult(ResponseBody responseBody, SseEmitter emitter, Long userId, String module) throws IOException {
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

                    boolean hasAnswer = data.contains("\"answer\"");
                    boolean hasDelta = data.contains("\"delta\"");
                    boolean hasNodeFinished = data.contains("\"event\":\"node_finished\"");

                    if (!hasAnswer && !hasDelta && !hasNodeFinished) {
                        continue;
                    }

                    if (hasNodeFinished) {
                        JsonNode root = parseStreamingJson(data);
                        if (root == null) {
                            continue;
                        }
                        String aiMode = root.path("data").path("process_data").path("model_name").asText(null);
                        JsonNode usage = extractUsageNode(root);
                        if (usage != null) {
                            recordUsageAsync(userId, module, aiMode, usage);
                        }
                        continue;
                    }

                    String chunk = extractStreamingChunkFast(data, hasAnswer, hasDelta);
                    if (chunk == null) {
                        JsonNode root = parseStreamingJson(data);
                        if (root == null) {
                            continue;
                        }
                        chunk = extractStreamingChunk(root);
                    }

                    if (!chunk.isBlank()) {
                        fullResult.append(chunk);
                        emitter.send(SseEmitter.event().data(chunk));
                    }
                }
            }
        } catch (Exception ex) {
            emitter.completeWithError(ex);
            throw ex instanceof IOException ? (IOException) ex : new IOException(ex.getMessage(), ex);
        }
        return fullResult.toString();
    }

    private JsonNode parseStreamingJson(String dataLine) {
        if (dataLine == null || dataLine.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(dataLine);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String extractStreamingChunk(JsonNode root) {
        if (root == null || root.isNull()) {
            return "";
        }
        JsonNode answer = root.get("answer");
        if (answer != null && !answer.isNull()) {
            return answer.asText();
        }
        JsonNode delta = root.get("delta");
        if (delta != null && !delta.isNull()) {
            return delta.asText();
        }
        return "";
    }

    private String extractStreamingChunkFast(String dataLine, boolean hasAnswer, boolean hasDelta) {
        if (dataLine == null || dataLine.isBlank()) {
            return "";
        }
        String key = hasAnswer ? "\"answer\"" : "\"delta\"";
        int keyIndex = dataLine.indexOf(key);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = dataLine.indexOf(':', keyIndex + key.length());
        if (colonIndex < 0) {
            return null;
        }
        int valueStart = dataLine.indexOf('"', colonIndex + 1);
        if (valueStart < 0) {
            return null;
        }
        int valueEnd = findJsonStringEnd(dataLine, valueStart + 1);
        if (valueEnd < 0) {
            return null;
        }
        String raw = dataLine.substring(valueStart + 1, valueEnd);
        return unescapeJsonString(raw);
    }

    private int findJsonStringEnd(String text, int start) {
        boolean escape = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private String unescapeJsonString(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        boolean escape = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!escape) {
                if (c == '\\') {
                    escape = true;
                    continue;
                }
                sb.append(c);
                continue;
            }
            escape = false;
            switch (c) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (i + 4 < raw.length()) {
                        String hex = raw.substring(i + 1, i + 5);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException ignore) {
                            sb.append('u');
                        }
                    } else {
                        sb.append('u');
                    }
                    break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    private JsonNode extractUsageNode(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        JsonNode usage = root.path("data").path("outputs").path("usage");
        if (!usage.isMissingNode() && !usage.isNull()) {
            return usage;
        }
        usage = root.path("data").path("process_data").path("usage");
        if (!usage.isMissingNode() && !usage.isNull()) {
            return usage;
        }
        usage = root.path("data").path("usage");
        if (!usage.isMissingNode() && !usage.isNull()) {
            return usage;
        }
        usage = root.path("usage");
        if (!usage.isMissingNode() && !usage.isNull()) {
            return usage;
        }
        return null;
    }

    @Override
    public void recordUsageAsync(Long userId, String module, String aiMode, BigDecimal totalPrice) {
        if (userId == null || totalPrice == null) {
            return;
        }
        AsyncManager.me().execute(new TimerTask() {
            @Override
            public void run() {
                FurnitureAiCallRecordsDO record = new FurnitureAiCallRecordsDO();
                record.setUserId(userId);
                record.setModule(module);
                record.setAiMode(aiMode);
                record.setTokenIn(BigDecimal.ZERO);
                record.setTokenOut(BigDecimal.ZERO);
                record.setTotalToken(BigDecimal.ZERO);
                record.setCost(totalPrice);
                record.setCreateTime(new Date());
                furnitureAiCallRecordsMapper.insert(record);
            }
        });
    }

    private void recordUsageAsync(Long userId, String module, String aiMode, JsonNode usageNode) {
        if (userId == null || usageNode == null || usageNode.isNull()) {
            return;
        }
        BigDecimal totalPrice = parseBigDecimal(usageNode.path("total_price"));
        recordUsageAsync(userId, module, aiMode, totalPrice);
    }

    private BigDecimal parseBigDecimal(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ignore) {
            return null;
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

        // 兼容外部图片链接：先下载到 profile/download 目录，再按既有流程处理
        if (isExternalHttpUrl(normalized)) {
            return downloadExternalImageToProfile(normalized);
        }

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

    private boolean isExternalHttpUrl(String path) {
        if (path == null) {
            return false;
        }
        String lower = path.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private File downloadExternalImageToProfile(String imageUrl) {
        File downloadDir = new File(RuoYiConfig.getProfile(), "download/image");
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            throw new ServiceException("创建下载目录失败: " + downloadDir.getAbsolutePath());
        }

        Request request = new Request.Builder()
                .url(imageUrl)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceException("下载外部图片失败: " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ServiceException("下载外部图片失败: 响应体为空");
            }

            String ext = resolveImageExtByMimeType(response.header("Content-Type"));
            String fileName = "ext_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            File targetFile = new File(downloadDir, fileName);
            Files.write(targetFile.toPath(), body.bytes());

            if (!targetFile.exists() || !targetFile.isFile()) {
                throw new ServiceException("下载外部图片失败: 文件落盘异常");
            }
            return targetFile;
        } catch (IOException e) {
            log.error("下载外部图片失败, url:{}, err:{}", imageUrl, e.getMessage(), e);
            throw new ServiceException("下载外部图片失败");
        }
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

    private void validateBalanceEnough(Long userId,BigDecimal cost) {
        FurnitureUserBalanceAccountDO account = furnitureUserBalanceAccountService.selectFurnitureUserBalanceAccountByUserId(userId);
        BigDecimal balance = account == null ? BigDecimal.ZERO : account.getBalance();
        if (balance == null || balance.compareTo(cost) < 0) {
            throw new ServiceException("余额不足");
        }
    }

}
