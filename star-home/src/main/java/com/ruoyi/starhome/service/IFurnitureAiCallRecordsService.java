package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageResp;

import java.util.List;

public interface IFurnitureAiCallRecordsService {
    FurnitureAiCallRecordsDO selectFurnitureAiCallRecordsById(Long id);

    FurnitureAiCallRecordsPageResp selectFurnitureAiCallRecordsList(FurnitureAiCallRecordsDO furnitureAiCallRecords);

    int insertFurnitureAiCallRecords(FurnitureAiCallRecordsDO furnitureAiCallRecords);

    int updateFurnitureAiCallRecords(FurnitureAiCallRecordsDO furnitureAiCallRecords);

    int deleteFurnitureAiCallRecordsByIds(Long[] ids);

    int deleteFurnitureAiCallRecordsById(Long id);
}
