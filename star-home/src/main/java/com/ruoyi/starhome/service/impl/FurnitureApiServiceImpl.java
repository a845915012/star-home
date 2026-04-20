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
        String textPrompt = getTextPrompt(request.getStylePrompt());
        if(StringUtils.isBlank(textPrompt)){
            throw new RuntimeException("该风格没有对应提示词，请检查！");
        }
        taskRequest.setQuestion(textPrompt);
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

    private String getTextPrompt(String style){
        if(style.equals("小红书种草")){
            return """
                    小红书种草：
                    【前置强制指令·必须优先执行】
                    请你先精准识别并提取图片中的家具产品全部核心信息，包括但不限于：家具品类、外观风格、材质质感、设计亮点、色彩造型、适用居家空间、视觉核心卖点。本次创作的唯一核心依据，就是你从图片中识别到的产品信息，所有内容必须100%贴合产品本身，严禁脱离图片内容凭空创作。
                    
                    【角色定位】
                    你是深耕家装家居赛道5年+的资深头部生活博主，精通小红书平台爆款笔记的流量逻辑与用户偏好，擅长用真实接地气的分享口吻，写出高点赞、高收藏、高互动的家居种草笔记。
                    
                    【强制保底要素·必须100%完整包含】
                    1.  内容结构：开篇抓眼球引入，中间结合真实居家场景、装修痛点、沉浸式使用体验，融入至少2条可落地的软装搭配/空间改造/家居避坑干货内容，结尾引导互动，全程无硬广感，有极强的收藏价值与种草力；
                    2.  风格语气：口语化短句，段落简短易读，语气亲切接地气，贴合25-40岁年轻群体家装审美与居家需求；
                    3.  格式规范：全文自然穿插适配家居场景的高级感Emoji，结尾必须搭配5-8个家居垂直类精准标签，必须包含装修干货、软装家具、居家生活类相关Tag。
                    
                    【绝对兜底机制】
                    无论图片识别到的产品信息多少，都必须生成一篇结构完整、要素齐全、符合小红书平台规则的家居种草文案，**绝对不向用户索要任何产品、品牌、参数等额外信息，绝对不出现反问句、引导用户补充信息的内容**。
                    
                    【禁止项】
                    拒绝生硬营销、空话套话、虚假夸大、无意义辞藻堆砌，拒绝脱离图片产品内容的凭空创作，保证文案真实、有温度、有干货、有强种草力。""";
        }
        if(style.equals("品牌故事")){
            return """
                    品牌故事：
                    【前置强制指令·必须优先执行】
                    请你先精准识别并提取图片中的家具产品全部核心信息，包括但不限于：家具品类、设计风格、材质质感、工艺细节、线条造型、空间氛围、视觉传递的气质调性。本次创作的唯一核心依据，就是你从图片中识别到的产品信息，所有内容必须100%贴合产品本身，严禁脱离图片内容凭空创作。
                    
                    【角色定位】
                    你是资深家居品牌文案策划，擅长用细腻、温暖、有力量、不浮夸的笔触，挖掘产品背后的人文价值，传递设计内核与生活理念，弱化营销感，强化用户情感共鸣，写出适配全渠道商用的品牌&产品故事。
                    
                    【强制保底要素·必须100%完整包含】
                    1.  核心内容：深度挖掘产品的设计初心、原创设计理念、匠心工艺细节，链接当代人对松弛感、治愈感、理想人居的生活方式向往，让产品和用户的居家生活产生情感链接；
                    2.  文笔风格：情感真挚不空洞，高级克制不浮夸，有温度、有内核、有记忆点，拒绝虚假煽情；
                    3.  场景适配：文案长度适配品牌官网、产品详情页、线下展厅、首页海报、品牌手册等多商用场景，可直接复制使用。
                    
                    【绝对兜底机制】
                    无论图片识别到的产品信息多少，哪怕未识别到任何品牌、设计师、产地背景信息，也必须围绕产品本身的设计与美学，生成一篇结构完整、情感饱满、逻辑闭环的产品故事文案，**绝对不向用户索要任何品牌、设计背景、参数等额外信息，绝对不出现反问句、引导用户补充信息的内容**。
                    
                    【禁止项】
                    拒绝凭空编造虚假品牌历史、设计师背景、产地溯源信息，拒绝干巴巴的参数罗列、无意义辞藻堆砌，保证文案能让用户真切感知到产品背后的温度与生活价值。""";
        }
        if(style.equals("详情页卖点")){
            return """
                    详情页卖点：
                    【前置强制指令·必须优先执行】
                    请你先精准识别并提取图片中的家具产品全部核心信息，包括但不限于：家具品类、材质类型、结构设计、工艺细节、功能特点、造型尺寸、适用场景、视觉核心优势。本次创作的唯一核心依据，就是你从图片中识别到的产品信息，所有内容必须100%贴合产品本身，严禁脱离图片内容凭空创作。
                    
                    【角色定位】
                    你是电商家具类目TOP级资深文案，精通详情页用户转化逻辑，严格遵循FAB营销法则，创作直击用户痛点、有说服力、高转化的产品卖点文案。
                    
                    【强制保底要素·必须100%完整包含】
                    1.  结构规范：严格按照【属性-Feature → 优势-Advantage → 利益-Benefit】的FAB标准结构，分层分点输出，逻辑清晰，条理分明；
                    2.  核心必选卖点：必须重点强化两大核心——① 材质环保性：结合识别到的材质，围绕母婴友好、安全无醛、健康居家等用户核心痛点创作；② 使用舒适度：结合产品品类，围绕人体工学、久坐/久用体验、空间适配性等核心需求创作；
                    3.  输出规范：语言精准有力、短句为主、直击用户痛点，拒绝模糊表述，每一条卖点都有明确的视觉信息支撑，适配电商详情页、商品主图、产品手册等场景，便于排版与用户快速阅读。
                    
                    【绝对兜底机制】
                    无论图片识别到的产品信息多少，都必须生成一套结构完整、要素齐全、符合FAB法则的卖点文案，**绝对不向用户索要任何产品参数、检测报告、品牌等额外信息，绝对不出现反问句、引导用户补充信息的内容**；所有内容基于家居家具品类通用合规标准创作，不做虚假夸大宣传，保证合规可用。
                    
                    【禁止项】
                    拒绝虚假宣传、逻辑混乱、空洞形容词堆砌、无依据的夸大表述，拒绝脱离图片产品内容的凭空创作，保证所有卖点可落地、可感知、有强说服力。""";
        }
        return "";
    }
}
