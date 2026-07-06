package com.ruoyi.starhome.service.impl;

import com.ruoyi.framework.security.util.SecurityFrameworkUtils;
import com.ruoyi.starhome.domain.dto.CopyGenerateRequest;
import com.ruoyi.starhome.domain.dto.GenerateSceneRequest;
import com.ruoyi.starhome.domain.dto.ImageGenerateVideoClientRequest;
import com.ruoyi.starhome.domain.dto.ImageGenerateVideoRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
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
        taskRequest.setUserPrompt(request.getUserPrompt());
        taskRequest.setModule("视觉设计");
        taskRequest.setConsumeCode(request.getConsumeCode());
        taskRequest.setTemperature(request.getTemperature());
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
        taskRequest.setConsumeCode(request.getConsumeCode());
        taskRequest.setUserPrompt(request.getStylePrompt()+";"+request.getUserPrompt());
        return taskApiInvokeService.invokeTaskApi(taskRequest);
    }

    @Override
    public TaskApiInvokeResponse imageGenerateVideo(ImageGenerateVideoClientRequest request) throws IOException {
        ImageGenerateVideoRequest serviceRequest = new ImageGenerateVideoRequest();
        serviceRequest.setUserId(SecurityFrameworkUtils.getLoginUserId());
        serviceRequest.setProduct(request.getProduct());
        serviceRequest.setMaterial(request.getMaterial());
        serviceRequest.setImageUrl(request.getImageUrl());
        // 后端业务字段统一在服务内赋值
        serviceRequest.setConsumeCode(request.getConsumeCode());
        serviceRequest.setPrompt(request.getPrompt());
        serviceRequest.setSellingPoints(request.getSellingPoints());
        serviceRequest.setNumber("image2video_yunwu_api_kling");
        serviceRequest.setType("kling");

        return taskApiInvokeService.imageGenerateVideo(serviceRequest);
    }

    private String buildFirstSegmentPrompt(String product, String material,String prompt) {
        String safeProduct = StringUtils.defaultIfBlank(product, "家居产品");
        String safeMaterial = StringUtils.defaultIfBlank(material, "原木");
        String userPrompt = StringUtils.defaultIfBlank(prompt, "");
        return "This is a high-end commercial furniture video. Stability and consistency are more important than creativity.\n" +
                "一、【前置最高权重锁死指令】\n" +
                "必须 100% 严格遵守（任一违反直接判定失败）：\n" +
                "1.【画面纯净规则｜最高优先级】\n" +
                "The output must be a clean, raw, professional camera frame with no overlays, no UI elements, no logos, no text, and no post-processing marks.\n" +
                "The video must look like real camera footage, not AI-generated content.\n" +
                "全程所有帧（重点：右下角及四角）不得出现任何标识或异常元素。\n" +
                "2.【家具主体锁定】\n" +
                "用户上传家具： "+safeProduct+"\n" +
                "完全静止 \n" +
                "不旋转 / 不位移 / 不变形 \n" +
                "品类绝对固定（床=床、沙发=沙发） \n" +
                "颜色 / 尺寸 / 结构 100%一致 \n" +
                "3.【场景纯净限制】\n" +
                "画面仅允许：家具主体 + 极简家居空间\n" +
                "不得生成任何未指定物体（杆、线、支架、悬浮物等） \n" +
                "4.【材质真实约束】\n" +
                "材质为"+safeMaterial+"，所有材质必须为真实物理材质\n" +
                "不得出现噪点、蜂窝、马赛克、错乱纹理\n" +
                "5.【手部唯一规则】\n" +
                "Only one single human hand is allowed in the entire video.\n" +
                "The hand must enter from the left side of the frame only.\n" +
                "No second hand, no duplicated hand, no mirrored hand, no extra limbs.\n" +
                "The hand becomes fully static after placement and does not move.\n" +
                "6.【焦点锁定】\n" +
                "镜头始终锁定：\n" +
                "家具正面核心接触面\n" +
                "禁止拍：底部 / 背面 / 内部 \n" +
                "7.【运镜约束】\n" +
                "仅允许：单一方向匀速长镜头\n" +
                "禁止：跳切 / 停顿 / 变速 \n" +
                "8.【音频】\n" +
                "完全静音 \n" +
                "9.【衔接静止区】\n" +
                "7–8秒：\n" +
                "禁止任何动态（尤其手部）\n" +
                "必须执行的生成标准\n" +
                "8K / 60fps / HDR / 商业级真实质感 \n" +
                "光源固定：5600K恒定柔光（无变化） \n" +
                "色彩：中性灰基底，饱和度 -15，对比度 +10，Gamma 2.2（全程一致） \n" +
                "运镜：滑轨匀速（加速度=0） \n" +
                "构图：家具中心 = 画面中心（绝对锁定） \n" +
                "画面：无噪点 / 无异常像素 / 无任何标识 \n" +
                "二、通用全局规范\n" +
                "1.主体 = 用户上传家具（1:1复刻） \n" +
                "2.核心展示 = 正面接触面 \n" +
                "3.背景 = 全程不变\n" +
                "4.时长 = 8秒（严格）\n" +
                "三、分镜\n" +
                "0–5秒（推镜）\n" +
                "从“用户原始图片”开始（完全一致） \n" +
                "正向匀速推镜（无加速） \n" +
                "焦点始终锁定家具 \n" +
                "手部：\n" +
                "2秒开始从左侧进入（慢速） \n" +
                "3秒末静止接触材质 \n" +
                "轻微滑动后静止\n" +
                "5–7秒（横移）\n" +
                "左 → 右 匀速平移 \n" +
                "速度与前段完全一致 \n" +
                "手部完全静止\n" +
                "7–8秒（关键优化）\n" +
                "手部缓慢移出画面（必须完全消失） \n" +
                "镜头回正 \n" +
                "景深复位 \n" +
                "最终帧 = 完全等同用户原始图片（像素级一致）";
    }

    @Override
    public SseEmitter createStream() {
        return taskApiInvokeService.createStream(SecurityFrameworkUtils.getLoginUserId());
    }

    private String getTextPrompt(String style){
        return switch (style) {
            case "小红书种草" -> """
                    【强制格式铁则（最高优先级，必须严格执行）】
                    1. 严格按照下方「输出结构」的顺序输出，每个大模块单独成行，正文4个段落各自单独成段，段落与段落之间空一行。
                    2. 绝对禁止输出“content”、注释说明、结构标记、序号括号等任何无关字符，只输出标题、正文、话题标签的纯文本内容。
                    3. 所有Emoji按要求放置，不要额外添加任何符号、分隔线。
                    
                    【输出结构（严格按此顺序）】
                    【标题】
                    15字以内，包含1-2个Emoji，突出家具的风格或情绪价值。
                    可用思路：数字冲击、痛点共鸣、场景代入，拒绝生硬广告感。
                    
                    【正文】（200-300字，固定4段，每段开头加1个契合内容的Emoji，段与段空行）
                    1. 开篇钩子：第一人称视角，用真实生活场景或具体痛点引入，引发读者共鸣
                    2. 产品亮点：结合家具外观，提炼3个具体亮点，分别讲颜值质感、材质细节、功能设计，不用“超级好看”“绝美”这类空泛词，用具体描述替代
                    3. 使用场景：描述家具放在家里的日常画面，营造氛围感，自然融入1条实用装修干货小知识
                    4. 互动话术：1句自然的引导评论/收藏话术，不生硬
                    
                    【话题标签】
                    6-8个话题标签，格式为 #标签名，必须包含 #家居好物 #装修攻略 #好物分享，再根据家具风格匹配对应标签
                    
                    【语气要求】
                    亲切真实，像闺蜜分享居家好物，用第一人称“我”的视角，有生活感，拒绝广告腔。
                    
                    【禁止项】
                    - 禁止所有内容连在一起不分段
                    - 禁止输出任何多余的英文、标记、注释文字
                    - 禁止使用空洞夸赞的形容词""";
            case "品牌故事" -> """
                    你是一位家居品牌的内容叙事专家，擅长用温暖细腻的文字讲述家具背后的设计理念与生活方式。
                    
                    请根据你看到的家具图片，写一篇品牌故事风格的文案。
                    
                    ## 输出结构
                    
                    【主标题】
                    一个有意境感的标题，10-15字，优雅有力，不堆砌形容词
                    
                    【正文】200-300字，需要换行，换行时增加\\n分为以下3段：
                    1. 设计灵感 —— 从视觉到心灵的旅程：描述这件家具给人的第一印象，它的线条、材质、色彩传递的情感
                    2. 工艺与匠心 —— 讲述材质选择和工艺细节背后的坚持，体现品牌对品质的追求
                    3. 生活想象 —— 描绘拥有这件家具后的生活场景：清晨的阳光、午后的静谧、夜晚的温馨
                    
                    【品牌结语】
                    1句品牌理念式收尾，余味悠长，让读者记住情绪而非功能
                    
                    ## 语气要求
                    - 温暖克制，像翻阅一本有质感的家居杂志
                    - 每段都是一个完整的小场景，代入感强
                    - 不堆砌华丽词藻，用具体细节打动人""";
            case "详情页卖点" -> """
                    你是一位资深电商文案策划，擅长提炼产品卖点并转化为打动消费者的购买理由。
                    
                    请根据你看到的家具图片，写一篇电商详情页的卖点文案。
                    
                    ## 输出结构
                    
                    【主卖点标题】
                    一个直击痛点的标题，12字以内，明确传达核心利益
                    
                    【FAB卖点展开】按以下格式输出3组卖点：
                    每组50-80字，用简洁有力的短句，需要换行，换行时增加\\n,格式为：属性 | 优势 | 利益
                    示例格式：
                    全实木框架 | 结构稳固承重强 | 孩子在上面蹦跳也不担心，用10年依旧稳当
                    
                    【场景描述】
                    80-100字，1段文字描绘产品在实际家居场景中的使用画面，让消费者能"看到"自己家的样子
                    
                    【参数速览】
                    用简短列表列出：材质、尺寸范围、颜色可选、适用空间、售后保障
                    
                    ## 语气要求
                    - 专业可信，像值得信赖的导购而非推销员
                    - 卖点要有数据感或对比感（如"比普通密度板耐用3倍"）
                    - 利益点要落到消费者的真实生活里，代入感强""";
            default -> "";
        };
    }
}
