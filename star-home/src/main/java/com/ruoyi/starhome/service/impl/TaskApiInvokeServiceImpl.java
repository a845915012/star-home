package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import com.ruoyi.starhome.domain.FurnitureNumberApiPoolDO;
import com.ruoyi.starhome.domain.FurnitureUserBalanceAccountDO;
import com.ruoyi.starhome.domain.dto.AiApiCallResult;
import com.ruoyi.starhome.domain.dto.FileInfo;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
import com.ruoyi.starhome.mapper.FurnitureAiCallRecordsMapper;
import com.ruoyi.starhome.mapper.FurnitureNumberApiPoolMapper;
import com.ruoyi.starhome.mapper.FurnitureUserPackageRightsMapper;
import com.ruoyi.starhome.service.IApiCallMonitorCacheService;
import com.ruoyi.starhome.service.IFurnitureUserBalanceAccountService;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
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

    private static final BigDecimal TASK_API_CONSUME_AMOUNT = new BigDecimal("9.9");

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
    private IApiCallMonitorCacheService IApiCallMonitorCacheService;

    @Autowired
    private IApiCallMonitorCacheService apiCallMonitorCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskApiInvokeResponse invokeTaskApi(TaskApiInvokeRequest request) {
        if (request == null || request.getUserId() == null || request.getApiNumber() == null) {
            throw new ServiceException("userId和apiNumber不能为空");
        }

        validateBalanceEnough(request.getUserId());

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
            JsonNode usageNode = mapper.readTree("{\"prompt_tokens\":\"0\",\"completion_tokens\":\"0\",\"total_tokens\":\"0\",\"total_price\":\"1\"}");
            recordUsageAsync(userId,module,"gemini_fusion_4imgs",usageNode);
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

    private void recordUsageAsync(Long userId, String module, String aiMode, JsonNode usageNode) {
        if (userId == null || usageNode == null || usageNode.isNull()) {
            return;
        }
        BigDecimal promptTokens = parseBigDecimal(usageNode.path("prompt_tokens"));
        BigDecimal completionTokens = parseBigDecimal(usageNode.path("completion_tokens"));
        BigDecimal totalTokens = parseBigDecimal(usageNode.path("total_tokens"));
        BigDecimal totalPrice = parseBigDecimal(usageNode.path("total_price"));
        AsyncManager.me().execute(new TimerTask() {
            @Override
            public void run() {
                FurnitureAiCallRecordsDO record = new FurnitureAiCallRecordsDO();
                record.setUserId(userId);
                record.setModule(module);
                record.setAiMode(aiMode);
                record.setTokenIn(promptTokens);
                record.setTokenOut(completionTokens);
                record.setTotalToken(totalTokens);
                record.setCost(totalPrice);
                record.setCreateTime(new Date());
                furnitureAiCallRecordsMapper.insert(record);
            }
        });
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

    private void validateBalanceEnough(Long userId) {
        FurnitureUserBalanceAccountDO account = furnitureUserBalanceAccountService.selectFurnitureUserBalanceAccountByUserId(userId);
        BigDecimal balance = account == null ? BigDecimal.ZERO : account.getBalance();
        if (balance == null || balance.compareTo(TASK_API_CONSUME_AMOUNT) < 0) {
            throw new ServiceException("余额不足");
        }
    }

}
