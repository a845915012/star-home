package com.ruoyi.starhome.service.admin.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageResp;
import com.ruoyi.starhome.domain.vo.FurnitureAiCallRecordsVO;
import com.ruoyi.starhome.mapper.admin.FurnitureAdminCallRecordsMapper;
import com.ruoyi.starhome.service.admin.IFurnitureAdminCallRecordsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FurnitureAdminCallRecordsServiceImpl implements IFurnitureAdminCallRecordsService {

    @Autowired
    private FurnitureAdminCallRecordsMapper furnitureAdminCallRecordsMapper;

    @Override
    public FurnitureAiCallRecordsPageResp selectCallRecordsPage(String module, String username, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<FurnitureAiCallRecordsVO> records = furnitureAdminCallRecordsMapper.selectPageWithUserAndModule(module, username);
        PageInfo<FurnitureAiCallRecordsVO> pageInfo = new PageInfo<>(records);

        FurnitureAiCallRecordsPageResp resp = new FurnitureAiCallRecordsPageResp();
        resp.setList(records);
        resp.setTotal(pageInfo.getTotal());
        resp.setPageNum(pageInfo.getPageNum());
        resp.setPageSize(pageInfo.getPageSize());
        resp.setPages(pageInfo.getPages());
        return resp;
    }
}
