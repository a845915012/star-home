package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageResp;

public interface IFurnitureAiCallRecordsService {

    FurnitureAiCallRecordsPageResp selectFurnitureAiCallRecordsList(FurnitureAiCallRecordsPageRequest request);

}
