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
        return "必须 100% 严格遵守的核心禁令（违反任意一条均视为生成失败）\n" +
                "1.【最高优先级・右下角水印专项绝对禁令】 全程全帧、画面所有区域（重点锁定画面右下角、四个边角），绝对禁止生成任何形式的水印、logo、标识、文字、角标、平台生成标记、AI 隐形水印、半透明标识、像素级水印、单帧闪烁标识、推广信息；画面全程 100% 纯净，无任何多余视觉元素，哪怕是单帧、半透明、像素级的微小水印也绝对禁止出现。\n" +
                "2."+safeProduct+"全程绝对静止，空间朝向、三维形态、品类结构、尺寸颜色 100% 与用户上传原图一致，无任何旋转、位移、形变、穿模、品类变更（沙发始终为沙发，床始终为床，床垫始终为床垫，无任何形态突变）\n" +
                "3.全程全帧画面绝对禁止生成任何用户未指定的不明物体，包括莫名杆子、金属条、线条、立柱、支架、悬浮结构、多余陈设，仅保留用户指定家具 + 固定极简家居空间\n" +
                "4.所有材质纹理必须还原真实物理肌理，绝对禁止蜂窝状、颗粒状、马赛克式、网格状的错乱噪点与畸变纹理\n" +
                "5.第一段仅自然放松的真人手部入画，绝对禁止生成模特头部、躯干、半身、残缺身体部位，手部仅静态放置在家具核心展示面，不遮挡产品主体，仅做视觉引导突出材质\n" +
                "6.全程镜头焦点严格锁定用户指定家具的正面核心使用接触面（床垫为正面面料层、沙发为坐面 + 靠背正面、柜体为柜门正面），绝对禁止聚焦到家具底部、床底、背面、内部框架、柜体深处等非核心展示区域\n" +
                "7.全程无任何高频运镜切换、景别跳变、画面骤停，采用连贯匀速长镜头，节奏平缓丝滑，无 AI 违和感\n" +
                "8.全程无任何人声配音、背景音乐、环境音效，纯视觉画面呈现 \n" +
                "9.无动态元素衔接禁令：衔接关键帧区间（第一段 7-8 秒、第二段 0-1 秒）绝对禁止出现手部及任何动态物体，家具全程保持静止，背景空间无任何变动\n" +
                "必须 100% 执行的核心生成标准\n" +
                "1.画质标准：8K 超高清分辨率，60 帧 / 秒高帧率，HDR 广色域，真人实拍级高端家居商业宣传片质感，无过曝、无暗部死黑、无画面抖动 \n" +
                "2.光影标准：全程采用 5600K 标准日光恒定柔光系统，主光、辅光、轮廓光的位置、强度、色温全程固定无变化，无明暗起伏、无动态光影跳变，柔光精准打亮家具正面核心接触面 \n" +
                "3.色彩标准：全程采用潘通中性灰基底，色彩饱和度统一 - 15，对比度统一 + 10，伽马值 2.2，全程无任何色彩偏移、滤镜变动 \n" +
                "4.运镜标准：全程采用专业电影级滑轨匀速运镜，加速度为 0，绝对匀速，无任何变速、启停、急推急拉，两段视频运镜节奏完全同频 \n" +
                "5.空间锚定：家具主体几何中心全程严格锁定在画面绝对中心轴线，无论运镜如何变化，家具中心永远与画面中心重合，无任何偏移；背景空间结构、墙体、地面、陈设全程 100% 固定不变 \n" +
                "6.画面纯净度标准：画面所有区域（尤其是右下角）全程无任何多余像素、异常色块、标识水印，帧帧纯净无杂质。\n" +
                "二、通用全局规范\n" +
                "1.主体还原：严格 1:1 还原用户上传图片中的家具外观，画面绝对核心为 **"+safeProduct+"，精准呈现"+safeMaterial+"的真实物理肌理，深度融入"+userPrompt+"** 核心要求，AI 不得做任何自主创意修改 \n" +
                "2.核心展示区锁定：全程以家具正面核心使用接触面为唯一核心展示区域，所有运镜、焦点变化均围绕该区域展开，无任何偏离 \n" +
                "3.场景规范：背景采用与家具风格统一的极简高级家居空间，空间调性、软装配色、陈设位置全程固定，无任何新增、删减、改动的元素 \n" +
                "4.衔接规范：两段视频的首尾衔接帧，锚点、光影、色彩、机位、背景空间、家具朝向 100% 匹配，通过景深平滑过渡实现无缝拼接，无任何画面跳切、断层 \n" +
                "5.时长规范：单条视频时长严格控制为 8 秒，全程匀速运镜，无提前收尾、画面骤停的情况 \n" +
                "5.零水印二次规范：视频从第一帧到最后一帧，画面右下角及所有边角，全程无任何水印、标识、文字，无任何例外。\n" +
                "0-5 秒画面以恒定速度平稳启动，采用与家具中心轴线垂直的正向轨道匀速微推连贯长镜头，全程单方向、单速度、无任何启停切换；从家具完整全景，平缓顺滑过渡到家具正面核心使用接触面的中近景，全程焦点始终锁定画面中心绝对静止的家具主体；第 2 秒末，仅自然放松的真人手部入画，无任何身体其他部位入画，手部以完全放松的姿态静态放置在家具核心材质区域的表面，全程无任何移动、划过、抚触动作，手部不遮挡家具核心主体，仅作为视觉引导突出材质肌理；全程光影恒定不变，画面干净无杂物、无多余物体，全程无水印，画面右下角无任何标识、水印\n" +
                "5-7 秒镜头平缓切换为与家具正面平行的横向短距离匀速滑轨平移（左→右）连贯长镜头，全程单方向、单速度、无任何启停切换，平移速度与前序推镜速度完全匹配，无节奏突变；手部全程保持原有静态姿态完全静止，无任何多余动作，持续放置在家具核心材质区域；镜头焦点跟随平移运镜，平缓顺滑地锁定家具核心材质的真实物理肌理，自然呈现材质细节（床垫针织面料编织纹理、皮革毛孔、木纹走向），无突兀特写跳变；家具全程绝对静止、形态恒定，背景空间固定不变，全程无水印，画面右下角无任何标识、水印\n" +
                "7-8 秒镜头平缓回归与家具中心轴线垂直的正向机位，手部以极缓匀速动作平稳自然出画，出画后画面无任何手部残留元素；同步启动渐进式景深平滑过渡，焦点从绝对静止的家具主体，以恒定速度缓慢向背景空间转移，家具主体从清晰逐步转为轻微虚化，背景空间从虚化逐步转为清晰；最终定格为背景空间完全清晰、家具主体轻微柔化、家具中心严格锁定画面中心、朝向形态与初始帧完全一致的稳定帧，全程保持恒定运镜惯性，无任何镜头停止、暗场、淡出，为第二段视频预留 100% 兼容的通用衔接入口，全程无水印，画面右下角无任何标识、水印";
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
