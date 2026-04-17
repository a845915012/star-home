package com.ruoyi.starhome.service.impl;

import com.ruoyi.framework.security.util.SecurityFrameworkUtils;
import com.ruoyi.starhome.domain.dto.CopyGenerateRequest;
import com.ruoyi.starhome.domain.dto.GenerateSceneRequest;
import com.ruoyi.starhome.domain.dto.ImageGenerateVideoClientRequest;
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
        taskRequest.setModule("视觉设计");
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
    public TaskApiInvokeResponse imageGenerateVideo(ImageGenerateVideoClientRequest request) throws IOException {
//        if (Boolean.TRUE.equals(request.getGenerateDescription())) {
//            TaskApiInvokeRequest taskRequest = new TaskApiInvokeRequest();
//            taskRequest.setUserId(SecurityFrameworkUtils.getLoginUserId());
//            taskRequest.setApiNumber("image2videotext");
//            taskRequest.setUseSse(false);
//            taskRequest.setQuestion("根据图片主体生成描述该主体的视频");
//            taskRequest.setModule("图生视频");
//            taskRequest.setConsumeConstants(ConsumeConstants.IMAGE2VIDEOTEXT);
//            taskRequest.setFilePaths(request.getImageUrls());
//
//            TaskApiInvokeResponse descriptionResponse = taskApiInvokeService.invokeTaskApi(taskRequest);
//            if (descriptionResponse != null && StringUtils.isNotBlank(descriptionResponse.getApiResult())) {
//                request.setPrompt(descriptionResponse.getApiResult());
//            }
//        }
        ImageGenerateVideoRequest serviceRequest = new ImageGenerateVideoRequest();
        serviceRequest.setUserId(SecurityFrameworkUtils.getLoginUserId());
        serviceRequest.setApiNumber(request.getApiNumber());
        serviceRequest.setProduct(request.getProduct());
        serviceRequest.setMaterial(request.getMaterial());
        serviceRequest.setImageUrls(request.getImageUrls());
        serviceRequest.setGenerateDescription(request.getGenerateDescription());
        // 后端业务字段统一在服务内赋值
        serviceRequest.setConsumeConstants(ConsumeConstants.IMAGE2VIDEO);
        serviceRequest.setPrompt(buildFirstSegmentPrompt(request.getProduct(), request.getMaterial()));

        return taskApiInvokeService.imageGenerateVideo(serviceRequest);
    }

    private String buildFirstSegmentPrompt(String product, String material) {
        String safeProduct = StringUtils.defaultIfBlank(product, "家居产品");
        String safeMaterial = StringUtils.defaultIfBlank(material, "原木");
        return "视频一（0——8s）：\n" +
                "生成高端家居商业视频，8K，HDR，60fps，暖色自然光，无水印无文字。\n" +
                "\n" +
                "主体：" + safeProduct + "（材质" + safeMaterial + "），始终位于画面中心，不变形、不漂移。\n" +
                "\n" +
                "【时间轴控制】\n" +
                "\n" +
                "0s:\n" +
                "固定镜头，全景（wide shot），" + safeProduct + "位于空间中央，光线稳定，环境完整展示\n" +
                "\n" +
                "1s:\n" +
                "镜头开始极慢速向前推进（dolly in），速度恒定，无加速\n" +
                "\n" +
                "2s:\n" +
                "推进继续，画面变为中景（medium shot），产品仍绝对居中\n" +
                "\n" +
                "3s:\n" +
                "人物进入画面边缘（不遮挡主体），轻微接触产品（如触摸/坐下）\n" +
                "\n" +
                "4s:\n" +
                "镜头继续推进，出现材质细节（" + safeMaterial + "纹理），浅景深增强\n" +
                "\n" +
                "5s:\n" +
                "轻微调整角度（极小幅度），但产品仍在画面正中心\n" +
                "\n" +
                "6s:\n" +
                "人物动作结束，退出画面或静止，焦点完全回到产品\n" +
                "\n" +
                "7s:\n" +
                "镜头减速，逐渐停止推进\n" +
                "\n" +
                "8s（关键锚点帧）:\n" +
                "完全静止画面：\n" +
                "- 中近景（medium-close shot）\n" +
                "- 平视角度（eye-level）\n" +
                "- 产品完美居中\n" +
                "- 无任何运动（人物/镜头/光影全部静止）\n" +
                "- 光线方向固定\n" +
                "\n" +
                "要求：\n" +
                "smooth motion, constant speed, fixed focal length, no lighting change";
    }

    @Override
    public SseEmitter createStream() {
        return taskApiInvokeService.createStream(SecurityFrameworkUtils.getLoginUserId());
    }
}
