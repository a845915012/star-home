package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureDesignTaskDO;
import com.ruoyi.starhome.mapper.FurnitureDesignTaskMapper;
import com.ruoyi.starhome.service.IFurnitureDesignTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureDesignTaskServiceImpl implements IFurnitureDesignTaskService {
    @Autowired
    private FurnitureDesignTaskMapper furnitureDesignTaskMapper;

    @Override
    public FurnitureDesignTaskDO selectFurnitureDesignTaskById(Long id) {
        return furnitureDesignTaskMapper.selectById(id);
    }

    @Override
    public List<FurnitureDesignTaskDO> selectFurnitureDesignTaskList(FurnitureDesignTaskDO furnitureDesignTask) {
        return furnitureDesignTaskMapper.selectList(new LambdaQueryWrapper<FurnitureDesignTaskDO>()
                .eq(furnitureDesignTask.getUserId() != null, FurnitureDesignTaskDO::getUserId, furnitureDesignTask.getUserId())
                .eq(furnitureDesignTask.getStatus() != null, FurnitureDesignTaskDO::getStatus, furnitureDesignTask.getStatus())
                .orderByDesc(FurnitureDesignTaskDO::getId));
    }

    @Override
    public int insertFurnitureDesignTask(FurnitureDesignTaskDO furnitureDesignTask) {
        return furnitureDesignTaskMapper.insert(furnitureDesignTask);
    }

    @Override
    public int updateFurnitureDesignTask(FurnitureDesignTaskDO furnitureDesignTask) {
        return furnitureDesignTaskMapper.updateById(furnitureDesignTask);
    }

    @Override
    public int deleteFurnitureDesignTaskByIds(Long[] ids) {
        return furnitureDesignTaskMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureDesignTaskById(Long id) {
        return furnitureDesignTaskMapper.deleteById(id);
    }
}
