package com.ruoyi.starhome.mapper;

import com.ruoyi.starhome.domain.FurnitureApiCallMonitor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 家居接口调用监控 数据层
 *
 * @author ruoyi
 */
@Mapper
public interface FurnitureApiCallMonitorMapper {
    /**
     * 查询家居接口调用监控
     *
     * @param id 主键ID
     * @return 家居接口调用监控
     */
    FurnitureApiCallMonitor selectFurnitureApiCallMonitorById(Long id);

    /**
     * 根据用户ID查询家居接口调用监控
     *
     * @param userId 用户ID
     * @return 家居接口调用监控
     */
    FurnitureApiCallMonitor selectFurnitureApiCallMonitorByUserId(Long userId);

    /**
     * 查询家居接口调用监控列表
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 家居接口调用监控集合
     */
    List<FurnitureApiCallMonitor> selectFurnitureApiCallMonitorList(FurnitureApiCallMonitor furnitureApiCallMonitor);

    /**
     * 新增家居接口调用监控
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 结果
     */
    int insertFurnitureApiCallMonitor(FurnitureApiCallMonitor furnitureApiCallMonitor);

    /**
     * 修改家居接口调用监控
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 结果
     */
    int updateFurnitureApiCallMonitor(FurnitureApiCallMonitor furnitureApiCallMonitor);

    /**
     * 删除家居接口调用监控
     *
     * @param id 主键ID
     * @return 结果
     */
    int deleteFurnitureApiCallMonitorById(Long id);

    /**
     * 批量删除家居接口调用监控
     *
     * @param ids 需要删除的主键ID
     * @return 结果
     */
    int deleteFurnitureApiCallMonitorByIds(Long[] ids);
}
