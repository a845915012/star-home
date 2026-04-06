package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageResp;

import java.util.List;

public interface IFurnitureVideoTaskService {

    FurnitureVideoTaskPageResp selectPage(FurnitureVideoTaskPageRequest request);

    List<FurnitureVideoTaskDO> listByGenerationTaskId(Long generationTaskId);

    String getProcessByTaskId(String taskId);

    void processAppendingGenerationTasks();
}
