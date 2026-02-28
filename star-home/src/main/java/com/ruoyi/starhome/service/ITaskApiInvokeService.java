package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;

public interface ITaskApiInvokeService {
    TaskApiInvokeResponse invokeTaskApi(TaskApiInvokeRequest request);
}
