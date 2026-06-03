package com.ruoyi.starhome.service;

import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.starhome.domain.FurnitureApiCallMonitor;

import java.util.List;

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
    TableDataInfo selectFurnitureApiCallMonitorPage(FurnitureApiCallMonitor furnitureApiCallMonitor);

}
