package com.ruoyi.starhome.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.mapper.FurnitureVideoTaskMapper;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class FurnitureVideoTaskProcessSyncTask {

    @Autowired
    private FurnitureVideoTaskMapper furnitureVideoTaskMapper;

    @Autowired
    private IFurnitureVideoTaskService furnitureVideoTaskService;

    /**
     * 每5分钟同步一次未完成视频任务进度
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void syncUnfinishedVideoTaskProcess() {
        List<FurnitureVideoTaskDO> unfinishedTasks = furnitureVideoTaskMapper.selectList(
                new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                        .eq(FurnitureVideoTaskDO::getIsComplete, 0)
                        .isNotNull(FurnitureVideoTaskDO::getTaskId)
                        .orderByAsc(FurnitureVideoTaskDO::getId)
        );

        if (unfinishedTasks == null || unfinishedTasks.isEmpty()) {
            return;
        }

        for (FurnitureVideoTaskDO task : unfinishedTasks) {
            String taskId = task.getTaskId();
            if (taskId == null || taskId.isBlank()) {
                continue;
            }
            try {
                furnitureVideoTaskService.getProcessByTaskId(taskId);
            } catch (Exception e) {
                log.error("同步视频任务进度失败, taskId={}", taskId, e);
            }
        }

        try {
            furnitureVideoTaskService.processAppendingGenerationTasks();
        } catch (Exception e) {
            log.error("处理appending单据头视频拼接失败", e);
        }
    }

    /**
     * 每5分钟同步一次未完成视频任务进度
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void syncAppendVideoTaskProcess() {
        try {
            furnitureVideoTaskService.processAppendingGenerationTasks();
        } catch (Exception e) {
            log.error("处理appending单据头视频拼接失败", e);
        }
    }
}
