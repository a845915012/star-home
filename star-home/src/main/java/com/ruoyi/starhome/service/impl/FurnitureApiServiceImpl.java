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
        serviceRequest.setPrompt(buildFirstSegmentPrompt(request.getProduct(), request.getMaterial(),request.getPrompt()));

        return taskApiInvokeService.imageGenerateVideo(serviceRequest);
    }

    private String buildFirstSegmentPrompt(String product, String material,String prompt) {
        String safeProduct = StringUtils.defaultIfBlank(product, "家居产品");
        String safeMaterial = StringUtils.defaultIfBlank(material, "原木");
        String userPrompt = StringUtils.defaultIfBlank(prompt, "");
        return "【最高优先级・不可逾越铁则】（视频必须 100% 严格遵守，生成权重拉满）\n" +
                "无多余不明物体强制禁令：全程全帧画面（含背景、边角、景深过渡区间、镜头切换区间），绝对禁止生成任何用户未指定的、无意义的不明物体，包括但不限于莫名其妙的杆子、金属条、线条、立柱、支架、不明结构、悬浮物体、穿模的家具部件、多余的陈设品、非用户上传的额外家具；画面内仅保留用户上传图片中的家具主体、固定统一的极简背景空间场景，无任何额外的、多余的、不明的物体出现，镜头切换、景别变化、景深过渡时必须严格遵守。 \n" +
                "家具主体形态全程恒定铁则："+safeProduct+"从第一帧到最后一帧，品类、形态、结构、尺寸、朝向、颜色、材质 100% 与用户上传图片完全一致，全程恒定不变。沙发全程为沙发，床全程为床，床垫全程为床垫，绝对禁止跨品类替换、形态变形、结构重构，无任何穿模、形变、错位。 \n" +
                "家具全程绝对静止铁则：家具主体在空间中全程保持绝对静止，仅镜头做专业电影级滑轨匀速运动，无任何旋转、翻转、位移、开合、伸缩等动态变化，严格遵循现实物理规则，彻底消除 AI 违和感。 \n" +
                "模特物理边界防穿模铁则（仅第一段生效）：\n" +
                "模特仅可在家具的正常使用合理位置活动（如沙发的座位区、床的床面边缘、柜体前方的站立区），绝对禁止出现在家具靠背内部、床体框架内、沙发坐垫下方等不合理位置\n" +
                "模特身体与家具保持真实物理边界，仅手部、背部、臀部等合理部位自然贴合家具表面，绝对禁止任何身体部位嵌入、融合、穿模到家具内部，无半透明、重叠、错位等违和效果\n" +
                "模特全程仅做自然静态动作，无任何多余动态，体态舒展自然，符合真人日常使用家具的正常姿态\n" +
                "材质纹理防错乱铁则：\n" +
                "所有材质特写必须还原真实物理纹理，绝对禁止生成蜂窝状、颗粒状、马赛克式、网格状的错乱噪点与畸变纹理\n" +
                "床垫面料：还原针织 / 天丝面料的细腻自然编织纹理，纤维走向清晰自然，无错乱填充\n" +
                "沙发材质：真皮还原自然细腻的皮革毛孔与不规则皮纹，布艺还原经纬分明的编织纹理，无畸变\n" +
                "木质 / 石材 / 金属材质：还原对应材质的原生自然肌理，无 AI 生成的虚假错乱纹理\n" +
                "若用户上传图片细节不清晰，默认生成对应品类家具的行业标准真实材质纹理，禁止用蜂窝噪点做模糊填充\n" +
                "零水印强制禁令：全程全帧画面无任何形式的水印、logo、标识、文字、角标，包括隐形水印、半透明标识、像素级水印，画面全程纯净无多余元素。 \n" +
                "无动态元素衔接禁令：衔接关键帧区间（第一段 7-8 秒、第二段 0-1 秒）绝对禁止出现模特及任何动态物体，家具全程保持静止，背景空间无任何变动，确保无缝拼接无断点、无新增不明物体。\n" +
                "【固定参数锁死规范（无缝衔接核心）】\n" +
                "主体还原规则：严格 1:1 还原用户上传图片中的家具外观，画面绝对核心为 **"+safeProduct+"，精准呈现"+safeMaterial+"的真实物理肌理，深度融入"+userPrompt+"** 核心要求，AI 不得做任何自主创意修改、形态重构。 \n" +
                "空间与视觉锚定：家具主体的几何中心，全程严格锁定在画面绝对中心轴线上，无论运镜如何变化，家具中心永远与画面中心重合，无任何偏移，空间结构、软装配色、陈设位置全程固定，无任何场景元素变动。 \n" +
                "画质与光影锁死：8K 超高清分辨率，60 帧 / 秒高帧率，HDR 广色域，真人实拍级商业宣传片质感；全程采用5600K 标准日光恒定柔光系统，主光、辅光、轮廓光的位置、强度、色温全程固定无变化，无过曝、无暗部死黑、无明暗起伏、无动态光影跳变，画面通透有层次，光影柔和不生硬，避免材质纹理出现反光畸变，杜绝光影错乱生成虚假线条 / 杆子。\n" +
                "色彩与参数统一：全程采用潘通中性灰基底，色彩饱和度统一 - 15，对比度统一 + 10，伽马值 2.2，全程无任何色彩偏移、滤镜变动；全程无任何人声配音、背景音乐、环境音效，纯视觉画面呈现。 \n" +
                "运镜铁则：全程采用专业电影级滑轨单组连贯匀速运镜，加速度为 0，绝对匀速，无任何镜头启停、无运镜方向突变、无景别跳变、无抖动、无卡顿、无急推急拉，完全模拟真人实拍的丝滑长镜头运动，单条 8 秒视频仅保留 2-3 组平缓运镜过渡，两段视频运镜节奏完全同频，运镜全程严格遵循透视规则，无错乱透视生成虚假线条 / 杆子。 \n" +
                "模特规范：模特根据家居场景风格自动适配，气质与场景调性完全统一，面容亲和、体态自然舒展，穿搭极简素雅，无夸张妆容、无怪异动作；全程仅做自然静态动作，无任何多余动态。\n" +
                "\n" +
                "0-4 秒面以恒定速度平稳启动，采用与家具中心轴线垂直的正向轨道匀速微推长镜头，全程单方向、单速度、无任何启停切换；从家具完整全景，平缓顺滑过渡到家具核心设计区域的中近景，全程焦点始终锁定画面中心绝对静止的家具主体；第 2 秒末，与场景风格适配的模特自然入镜，仅出现在家具正常使用的合理位置，上半身入画、体态自然舒展，手部以放松的姿态自然静态放置在家具核心材质区域的表面，全程无任何移动、划过、抚触动作，身体与家具保持真实物理边界，无穿模、无嵌入、无融合，不遮挡产品主体；全程画面干净无杂物、无水印、无任何不明杆子 / 多余物体，光影恒定不变4-6 秒镜头平缓切换为与家具正面平行的横向短距离匀速滑轨平移（左→右）长镜头，全程单方向、单速度、无任何启停切换，平移速度与前序推镜速度完全匹配，无节奏突变；模特全程保持原有静态姿态完全静止，手部持续自然放置在家具表面，无任何多余动作；镜头焦点跟随平移运镜，平缓顺滑地锁定材质的真实物理肌理，无蜂窝状、颗粒状错乱噪点，自然呈现材质细节，无突兀特写跳变；家具全程绝对静止、形态恒定，背景空间固定不变，无任何新增物体，画面无水印、无结构变形\n" +
                "6-8 秒镜头平缓回归与家具中心轴线垂直的正向机位，采用匀速缓拉长镜头，全程单方向、单速度、无任何启停切换；第 6-7.5 秒，模特以极缓的匀速动作平稳自然出画，全程无突兀动作、无画面遮挡，出画后画面内无任何模特残留元素；同步启动渐进式景深平滑过渡，焦点从绝对静止的家具主体，以恒定速度缓慢向背景空间转移，家具主体从清晰逐步转为轻微虚化，背景空间从虚化逐步转为清晰，全程背景空间结构、陈设 100% 固定不变，无任何新增物体；第 7.5-8 秒，画面定格为背景空间完全清晰、家具主体轻微柔化、家具中心严格锁定画面中心、朝向与形态与初始帧完全一致的稳定帧，全程保持恒定运镜惯性，无任何镜头停止、暗场、淡出，画面全程无水印、无任何动态元素，该视频的最后一帧作为第二个视频的入口，要为第二段视频预留 100% 兼容的通用衔接入口";
    }

    @Override
    public SseEmitter createStream() {
        return taskApiInvokeService.createStream(SecurityFrameworkUtils.getLoginUserId());
    }

    private String getTextPrompt(String style){
        return switch (style) {
            case "小红书种草" -> """
                    小红书种草：
                    【前置强制指令·必须优先执行】
                    回复的内容必须是简体中文，请你先精准识别并提取图片中的家具产品全部核心信息，包括但不限于：家具品类、外观风格、材质质感、设计亮点、色彩造型、适用居家空间、视觉核心卖点。本次创作的唯一核心依据，就是你从图片中识别到的产品信息，所有内容必须100%贴合产品本身，严禁脱离图片内容凭空创作。
                    
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
            case "品牌故事" -> """
                    品牌故事：
                    【前置强制指令·必须优先执行】
                    回复的内容必须是简体中文，请你先精准识别并提取图片中的家具产品全部核心信息，包括但不限于：家具品类、设计风格、材质质感、工艺细节、线条造型、空间氛围、视觉传递的气质调性。本次创作的唯一核心依据，就是你从图片中识别到的产品信息，所有内容必须100%贴合产品本身，严禁脱离图片内容凭空创作。
                    
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
            case "详情页卖点" -> """
                    详情页卖点：
                    【前置强制指令·必须优先执行】
                    回复的内容必须是简体中文，请你先精准识别并提取图片中的家具产品全部核心信息，包括但不限于：家具品类、材质类型、结构设计、工艺细节、功能特点、造型尺寸、适用场景、视觉核心优势。本次创作的唯一核心依据，就是你从图片中识别到的产品信息，所有内容必须100%贴合产品本身，严禁脱离图片内容凭空创作。
                    
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
            default -> "";
        };
    }
}
