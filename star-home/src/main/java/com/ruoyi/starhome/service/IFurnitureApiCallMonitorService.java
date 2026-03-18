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
     * 查询家居接口调用监控
     *
     * @param id 主键ID
     * @return 家居接口调用监控
     */
    public FurnitureApiCallMonitor selectFurnitureApiCallMonitorById(Long id);

    /**
     * 根据用户ID查询家居接口调用监控
     *
     * @param userId 用户ID
     * @return 家居接口调用监控
     */
    public FurnitureApiCallMonitor selectFurnitureApiCallMonitorByUserId(Long userId);

    /**
     * 查询家居接口调用监控列表
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 家居接口调用监控集合
     */
    public List<FurnitureApiCallMonitor> selectFurnitureApiCallMonitorList(FurnitureApiCallMonitor furnitureApiCallMonitor);

    /**
     * 分页查询家居接口调用监控（合并缓存数据）
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 分页结果
     */
    public TableDataInfo selectFurnitureApiCallMonitorPage(FurnitureApiCallMonitor furnitureApiCallMonitor);

    /**
     * 新增家居接口调用监控
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 结果
     */
    public int insertFurnitureApiCallMonitor(FurnitureApiCallMonitor furnitureApiCallMonitor);

    /**
     * 修改家居接口调用监控
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 结果
     */
    public int updateFurnitureApiCallMonitor(FurnitureApiCallMonitor furnitureApiCallMonitor);

    /**
     * 批量删除家居接口调用监控
     *
     * @param ids 需要删除的主键ID
     * @return 结果
     */
    public int deleteFurnitureApiCallMonitorByIds(Long[] ids);

    /**
     * 删除家居接口调用监控信息
     *
     * @param id 主键ID
     * @return 结果
     */
    public int deleteFurnitureApiCallMonitorById(Long id);
}
