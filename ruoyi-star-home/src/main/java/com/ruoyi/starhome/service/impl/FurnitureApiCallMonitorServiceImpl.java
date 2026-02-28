package com.ruoyi.starhome.service.impl;

import com.ruoyi.starhome.domain.FurnitureApiCallMonitor;
import com.ruoyi.starhome.mapper.FurnitureApiCallMonitorMapper;
import com.ruoyi.starhome.service.IFurnitureApiCallMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 家居接口调用监控 服务层实现
 *
 * @author ruoyi
 */
@Service
public class FurnitureApiCallMonitorServiceImpl implements IFurnitureApiCallMonitorService {
    @Autowired
    private FurnitureApiCallMonitorMapper furnitureApiCallMonitorMapper;

    /**
     * 查询家居接口调用监控
     *
     * @param id 主键ID
     * @return 家居接口调用监控
     */
    @Override
    public FurnitureApiCallMonitor selectFurnitureApiCallMonitorById(Long id) {
        return furnitureApiCallMonitorMapper.selectFurnitureApiCallMonitorById(id);
    }

    /**
     * 根据用户ID查询家居接口调用监控
     *
     * @param userId 用户ID
     * @return 家居接口调用监控
     */
    @Override
    public FurnitureApiCallMonitor selectFurnitureApiCallMonitorByUserId(Long userId) {
        return furnitureApiCallMonitorMapper.selectFurnitureApiCallMonitorByUserId(userId);
    }

    /**
     * 查询家居接口调用监控列表
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 家居接口调用监控
     */
    @Override
    public List<FurnitureApiCallMonitor> selectFurnitureApiCallMonitorList(FurnitureApiCallMonitor furnitureApiCallMonitor) {
        return furnitureApiCallMonitorMapper.selectFurnitureApiCallMonitorList(furnitureApiCallMonitor);
    }

    /**
     * 新增家居接口调用监控
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 结果
     */
    @Override
    public int insertFurnitureApiCallMonitor(FurnitureApiCallMonitor furnitureApiCallMonitor) {
        return furnitureApiCallMonitorMapper.insertFurnitureApiCallMonitor(furnitureApiCallMonitor);
    }

    /**
     * 修改家居接口调用监控
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 结果
     */
    @Override
    public int updateFurnitureApiCallMonitor(FurnitureApiCallMonitor furnitureApiCallMonitor) {
        return furnitureApiCallMonitorMapper.updateFurnitureApiCallMonitor(furnitureApiCallMonitor);
    }

    /**
     * 批量删除家居接口调用监控
     *
     * @param ids 需要删除的主键ID
     * @return 结果
     */
    @Override
    public int deleteFurnitureApiCallMonitorByIds(Long[] ids) {
        return furnitureApiCallMonitorMapper.deleteFurnitureApiCallMonitorByIds(ids);
    }

    /**
     * 删除家居接口调用监控信息
     *
     * @param id 主键ID
     * @return 结果
     */
    @Override
    public int deleteFurnitureApiCallMonitorById(Long id) {
        return furnitureApiCallMonitorMapper.deleteFurnitureApiCallMonitorById(id);
    }
}
