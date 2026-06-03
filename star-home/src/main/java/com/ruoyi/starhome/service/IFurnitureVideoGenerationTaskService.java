package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.dto.FurnitureVideoGenerationTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoGenerationTaskPageResp;

public interface IFurnitureVideoGenerationTaskService {

    FurnitureVideoGenerationTaskPageResp selectPage(FurnitureVideoGenerationTaskPageRequest request);
}
