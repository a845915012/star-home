package com.ruoyi.starhome.service.impl;

import com.ruoyi.framework.security.util.SecurityFrameworkUtils;
import com.ruoyi.starhome.domain.dto.CopyGenerateRequest;
import com.ruoyi.starhome.domain.dto.GenerateSceneRequest;
import com.ruoyi.starhome.domain.dto.ImageGenerateVideoRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
import com.ruoyi.starhome.enums.ConsumeConstants;
import com.ruoyi.starhome.service.IFurnitureApiService;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
public class FurnitureApiServiceImpl implements IFurnitureApiService {

    @Autowired
    private ITaskApiInvokeService taskApiInvokeService;

    @Override
    public TaskApiInvokeResponse imageGenerateScene(GenerateSceneRequest request) throws IOException {
        TaskApiInvokeRequest taskRequest = new TaskApiInvokeRequest();
        taskRequest.setUserId(SecurityFrameworkUtils.getLoginUserId());
        taskRequest.setApiNumber(request.getApiNumber());
        taskRequest.setUseSse(false);
        taskRequest.setFilePaths(request.getFilePaths());
        StringBuilder question = new StringBuilder();
        question.append(request.getStylePrompt());
        if (StringUtils.isNotBlank(request.getUserPrompt())) {
            question.append(request.getUserPrompt());
        }
        if (StringUtils.isNotBlank(request.getViewPrompt())) {
            question.append(request.getViewPrompt());
        }
        taskRequest.setQuestion(question.toString());
        taskRequest.setModule("图生图");
        taskRequest.setConsumeConstants(ConsumeConstants.IMAGE2IMAGE_FINAL);
        TaskApiInvokeResponse response = taskApiInvokeService.invokeGeminiImageApi(taskRequest);
        response.setApiResult(response.getApiResult());
        return response;
    }

    @Override
    public TaskApiInvokeResponse copyGenerate(CopyGenerateRequest request) {
        TaskApiInvokeRequest taskRequest = new TaskApiInvokeRequest();
        taskRequest.setUserId(SecurityFrameworkUtils.getLoginUserId());
        taskRequest.setApiNumber(request.getApiNumber());
        taskRequest.setUseSse(true);
        taskRequest.setFilePaths(request.getFilePaths());
        StringBuilder question = new StringBuilder();
        question.append(request.getStylePrompt());
        if (StringUtils.isNotBlank(request.getUserPrompt())) {
            question.append(request.getUserPrompt());
        }
        taskRequest.setQuestion(question.toString());
        taskRequest.setModule("灵感文案");
        taskRequest.setConsumeConstants(ConsumeConstants.IMAGE2TEXT);
        return taskApiInvokeService.invokeTaskApi(taskRequest);
    }

    @Override
    public TaskApiInvokeResponse imageGenerateVideo(ImageGenerateVideoRequest request) throws IOException {
        if (Boolean.TRUE.equals(request.getGenerateDescription())) {
            TaskApiInvokeRequest taskRequest = new TaskApiInvokeRequest();
            taskRequest.setUserId(SecurityFrameworkUtils.getLoginUserId());
            taskRequest.setApiNumber("image2videotext");
            taskRequest.setUseSse(false);
            taskRequest.setQuestion("根据图片主体生成描述该主体的视频");
            taskRequest.setModule("图生视频");
            taskRequest.setConsumeConstants(ConsumeConstants.IMAGE2VIDEOTEXT);
            taskRequest.setFilePaths(request.getImageUrls());

            TaskApiInvokeResponse descriptionResponse = taskApiInvokeService.invokeTaskApi(taskRequest);
            if (descriptionResponse != null && StringUtils.isNotBlank(descriptionResponse.getApiResult())) {
                request.setPrompt(descriptionResponse.getApiResult());
            }
        }
        return taskApiInvokeService.imageGenerateVideo(request);
    }

    @Override
    public SseEmitter createStream() {
        return taskApiInvokeService.createStream(SecurityFrameworkUtils.getLoginUserId());
    }
}
