package com.ruoyi.starhome.service;

import com.ruoyi.common.core.domain.PageResult;
import com.ruoyi.starhome.domain.FurnitureApiCallMonitor;

/**
 * 家居接口调用监控 服务层
 *
 * @author ruoyi
 */
public interface IFurnitureApiCallMonitorService {

    /**
     * 分页查询家居接口调用监控（合并缓存数据）
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 分页结果
     */
    PageResult<FurnitureApiCallMonitor> selectFurnitureApiCallMonitorPage(FurnitureApiCallMonitor furnitureApiCallMonitor);

}
