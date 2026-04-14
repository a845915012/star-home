package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.starhome.domain.FurnitureVideoGenerationTaskDO;
import com.ruoyi.starhome.domain.dto.FurnitureVideoGenerationTaskPageItemResp;
import com.ruoyi.starhome.domain.dto.FurnitureVideoGenerationTaskPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureVideoGenerationTaskPageResp;
import com.ruoyi.starhome.mapper.FurnitureVideoGenerationTaskMapper;
import com.ruoyi.starhome.service.IFurnitureVideoGenerationTaskService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FurnitureVideoGenerationTaskServiceImpl implements IFurnitureVideoGenerationTaskService {

    @Autowired
    private FurnitureVideoGenerationTaskMapper furnitureVideoGenerationTaskMapper;

    @Override
    public FurnitureVideoGenerationTaskPageResp selectPage(FurnitureVideoGenerationTaskPageRequest request) {
        Long userId = SecurityUtils.getUserId();

        try (com.github.pagehelper.Page<Object> page = PageHelper.startPage(request.getPageNum(), request.getPageSize())) {
            List<FurnitureVideoGenerationTaskDO> records = furnitureVideoGenerationTaskMapper.selectList(
                    StringUtils.isNotBlank(request.getStatus()) && request.getStatus().equalsIgnoreCase("process") ?
                    new LambdaQueryWrapper<FurnitureVideoGenerationTaskDO>()
                            .eq(FurnitureVideoGenerationTaskDO::getUserId, userId)
                            .in(FurnitureVideoGenerationTaskDO::getStatus,"process","appending")
                            .orderByDesc(FurnitureVideoGenerationTaskDO::getId)
                    : new LambdaQueryWrapper<FurnitureVideoGenerationTaskDO>()
                            .eq(FurnitureVideoGenerationTaskDO::getUserId, userId)
                            .eq(StringUtils.isNotBlank(request.getStatus()), FurnitureVideoGenerationTaskDO::getStatus ,request.getStatus().trim())
                            .orderByDesc(FurnitureVideoGenerationTaskDO::getId)
            );

            FurnitureVideoGenerationTaskPageResp resp = new FurnitureVideoGenerationTaskPageResp();
            resp.setTotal(page.getTotal());
            resp.setList(convertList(records));
            return resp;
        }
    }

    private List<FurnitureVideoGenerationTaskPageItemResp> convertList(List<FurnitureVideoGenerationTaskDO> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::convertItem).collect(Collectors.toList());
    }

    private FurnitureVideoGenerationTaskPageItemResp convertItem(FurnitureVideoGenerationTaskDO item) {
        FurnitureVideoGenerationTaskPageItemResp resp = new FurnitureVideoGenerationTaskPageItemResp();
        BeanUtils.copyProperties(item, resp);
        return resp;
    }
}
