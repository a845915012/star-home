package com.ruoyi.starhome.service.admin;

import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageResp;

public interface IFurnitureAdminCallRecordsService {

    FurnitureAiCallRecordsPageResp selectCallRecordsPage(String module, String username, Integer pageNum, Integer pageSize);

}
