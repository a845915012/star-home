package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsOverview;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageResp;

public interface IFurnitureAiCallRecordsService {

    FurnitureAiCallRecordsOverview selectFurnitureAiCallRecordsOverview(Long userId, String timeRange);

    FurnitureAiCallRecordsPageResp selectFurnitureAiCallRecordsList(Long userId, String timeRange, Integer pageNum, Integer pageSize);

    FurnitureAiCallRecordsPageResp selectFurnitureAiCallRecordsHistoryPage(Long userId, Integer pageNum, Integer pageSize);

}
