package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureDesignTaskDO;

import java.util.List;

public interface IFurnitureDesignTaskService {
    FurnitureDesignTaskDO selectFurnitureDesignTaskById(Long id);

    List<FurnitureDesignTaskDO> selectFurnitureDesignTaskList(FurnitureDesignTaskDO furnitureDesignTask);

    int insertFurnitureDesignTask(FurnitureDesignTaskDO furnitureDesignTask);

    int updateFurnitureDesignTask(FurnitureDesignTaskDO furnitureDesignTask);

    int deleteFurnitureDesignTaskByIds(Long[] ids);

    int deleteFurnitureDesignTaskById(Long id);
}
