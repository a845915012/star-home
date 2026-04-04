package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageResp;

public interface IFurnitureVideoTaskService {

    FurnitureVideoTaskPageResp selectPage(FurnitureVideoTaskPageRequest request);

    String getProcessByTaskId(String taskId);

    void processAppendingGenerationTasks();
}
