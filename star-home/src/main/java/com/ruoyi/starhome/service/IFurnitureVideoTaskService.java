package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageResp;
import com.ruoyi.starhome.domain.dto.VimaxVideoCallbackRequest;

import java.util.List;

public interface IFurnitureVideoTaskService {

    FurnitureVideoTaskPageResp selectPage(FurnitureVideoTaskPageRequest request);

    List<FurnitureVideoTaskDO> listByGenerationTaskId(Long generationTaskId);

    /**
     * 处理 vimax-agent 视频任务回调，替代原有轮询模式
     */
    void handleVideoTaskCallback(VimaxVideoCallbackRequest request);

}
