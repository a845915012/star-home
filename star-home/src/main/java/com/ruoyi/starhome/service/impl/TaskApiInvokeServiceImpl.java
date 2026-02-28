package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.domain.FurnitureNumberApiPoolDO;
import com.ruoyi.starhome.domain.FurnitureUserPackageRightsDO;
import com.ruoyi.starhome.domain.dto.AiApiCallResult;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
import com.ruoyi.starhome.mapper.FurnitureNumberApiPoolMapper;
import com.ruoyi.starhome.mapper.FurnitureUserPackageRightsMapper;
import com.ruoyi.starhome.service.ApiCallMonitorCacheService;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class TaskApiInvokeServiceImpl implements ITaskApiInvokeService {

    @Autowired
    private FurnitureUserPackageRightsMapper furnitureUserPackageRightsMapper;

    @Autowired
    private FurnitureNumberApiPoolMapper furnitureNumberApiPoolMapper;

    @Autowired
    private ApiCallMonitorCacheService apiCallMonitorCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskApiInvokeResponse invokeTaskApi(TaskApiInvokeRequest request) {
        if (request == null || request.getUserId() == null || request.getApiNumber() == null) {
            throw new ServiceException("userId和apiNumber不能为空");
        }

        Date now = new Date();
        FurnitureUserPackageRightsDO rights = furnitureUserPackageRightsMapper.selectOne(
                new LambdaQueryWrapper<FurnitureUserPackageRightsDO>()
                        .eq(FurnitureUserPackageRightsDO::getUserId, request.getUserId())
                        .eq(FurnitureUserPackageRightsDO::getIsActive, 1)
                        .and(w -> w.isNull(FurnitureUserPackageRightsDO::getExpireTime)
                                .or().ge(FurnitureUserPackageRightsDO::getExpireTime, now))
                        .orderByDesc(FurnitureUserPackageRightsDO::getId)
                        .last("limit 1")
        );

        if (rights == null) {
            throw new ServiceException("用户没有可用套餐权益");
        }

        Integer remainingCalls = rights.getRemainingCalls();
        boolean unlimited = remainingCalls != null && remainingCalls == -1;
        if (!unlimited && (remainingCalls == null || remainingCalls <= 0)) {
            throw new ServiceException("剩余调用次数不足");
        }

        AiApiCallResult callResult = callAiApiByApiNumber(request.getApiNumber());

        // 扣减次数
        rights.setUsedCalls(rights.getUsedCalls() == null ? 1 : rights.getUsedCalls() + 1);
        if (!unlimited) {
            rights.setRemainingCalls(remainingCalls - 1);
        }
        furnitureUserPackageRightsMapper.updateById(rights);

        // 调用监控先写缓存
        apiCallMonitorCacheService.recordCall(request.getUserId());

        TaskApiInvokeResponse response = new TaskApiInvokeResponse();
        response.setUserId(request.getUserId());
        response.setApiNumber(request.getApiNumber());
        response.setCallCost(callResult.getCallCost());
        response.setApiResult(callResult.getApiResult());
        response.setUsedCalls(rights.getUsedCalls());
        response.setRemainingCalls(unlimited ? -1 : rights.getRemainingCalls());
        return response;
    }

    /**
     * 按 apiNumber 查询对应接口配置。
     * 说明：这里只做配置查询占位，后续真实AI调用业务由你补充。
     */
    private AiApiCallResult callAiApiByApiNumber(Integer apiNumber) {
        FurnitureNumberApiPoolDO apiPool = furnitureNumberApiPoolMapper.selectOne(
                new LambdaQueryWrapper<FurnitureNumberApiPoolDO>()
                        .eq(FurnitureNumberApiPoolDO::getNumber, String.valueOf(apiNumber))
                        .last("limit 1")
        );

        if (apiPool == null) {
            throw new ServiceException("未找到apiNumber对应的接口配置: " + apiNumber);
        }

        AiApiCallResult result = new AiApiCallResult();
        result.setCallCost(BigDecimal.ZERO);
        result.setApiResult("TODO: 已查询到接口配置，后续补充真实AI调用逻辑。apiUrl=" + apiPool.getApiUrl());
        return result;
    }
}
