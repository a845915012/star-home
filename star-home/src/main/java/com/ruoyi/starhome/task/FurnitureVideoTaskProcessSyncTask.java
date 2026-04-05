package com.ruoyi.starhome.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.mapper.FurnitureVideoTaskMapper;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class FurnitureVideoTaskProcessSyncTask {

    /**
     * 锁超时分钟数：
     * 若一条任务的 processing=1 且 lock_time 早于当前时间 - lockExpireMinutes，
     * 会被视为“可能僵死锁”并在下一轮调度中回收。
     */
    @Value("${starhome.video-task.lock-expire-minutes:10}")
    private long lockExpireMinutes;

    /**
     * 锁心跳续租间隔（秒）：
     * 注意这里的“心跳”是数据库锁心跳，不是业务平台任务进度心跳。
     * 只要当前线程仍在处理任务，就会周期性刷新 lock_time，防止被误回收。
     */
    @Value("${starhome.video-task.lock-heartbeat-seconds:60}")
    private long lockHeartbeatSeconds;

    private final AtomicInteger heartbeatThreadIdx = new AtomicInteger(1);

    /**
     * 共享心跳线程池：
     * - 避免“每个任务创建一个线程池”的开销；
     * - 每个正在处理的任务只注册一个定时续租任务（ScheduledFuture）。
     */
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "video-task-lock-heartbeat-" + heartbeatThreadIdx.getAndIncrement());
        t.setDaemon(true);
        return t;
    });

    @Autowired
    private FurnitureVideoTaskMapper furnitureVideoTaskMapper;

    @Autowired
    private IFurnitureVideoTaskService furnitureVideoTaskService;

    /**
     * 每1分钟同步一次未完成视频任务进度。
     *
     * 处理流程：
     * 1) 先回收超时锁（仅回收未完成任务），防止异常退出导致锁永久不释放；
     * 2) 拉取未完成且未加锁任务；
     * 3) 逐条执行“先抢占锁，再处理业务”；
     * 4) 业务执行期间通过心跳续租 lock_time；
     * 5) 结束后按“条件释放”解锁，避免误释放他人已接管的锁。
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void syncUnfinishedVideoTaskProcess() {
        try {
            // 回收僵死锁：仅针对未完成任务，且 lock_time 超过阈值
            Date expireTime = secondPrecisionDate(System.currentTimeMillis() - lockExpireMinutes * 60 * 1000L);
            int released = furnitureVideoTaskMapper.releaseExpiredLocks(expireTime);
            if (released > 0) {
                log.warn("检测到超时任务锁并已释放, count={}", released);
            }
        } catch (Exception e) {
            log.error("释放超时任务锁失败", e);
        }

        List<FurnitureVideoTaskDO> unfinishedTasks = furnitureVideoTaskMapper.selectList(
                new LambdaQueryWrapper<FurnitureVideoTaskDO>()
                        .eq(FurnitureVideoTaskDO::getIsComplete, 0)
                        .eq(FurnitureVideoTaskDO::getProcessing, 0)
                        .isNotNull(FurnitureVideoTaskDO::getTaskId)
                        .orderByAsc(FurnitureVideoTaskDO::getId)
        );

        if (unfinishedTasks == null || unfinishedTasks.isEmpty()) {
            return;
        }

        for (FurnitureVideoTaskDO task : unfinishedTasks) {
            if (task == null || task.getId() == null) {
                continue;
            }

            String taskId = task.getTaskId();
            if (taskId == null || taskId.isBlank()) {
                continue;
            }

            int claimed = 0;
            // lockTimeRef 作为“锁令牌”：每次续租成功都会更新，finally 用最后一次成功值做条件释放
            AtomicReference<Date> lockTimeRef = new AtomicReference<>();
            ScheduledFuture<?> heartbeatFuture = null;
            try {
                // 抢占锁：仅当 processing=0 时成功，确保同一时刻只有一个执行者进入业务逻辑
                Date claimedLockTime = secondPrecisionDate();
                log.info("taskId:{},claimedLockTime:{}", taskId, claimedLockTime);
                claimed = furnitureVideoTaskMapper.claimTaskById(task.getId(), claimedLockTime);
                if (claimed <= 0) {
                    continue;
                }
                lockTimeRef.set(claimedLockTime);

                // 心跳间隔保护：
                // - 下限 5s，避免配置过小造成数据库压力；
                // - 上限为过期时间的 1/3 左右，保证在超时前至少有多次续租机会。
                long heartbeatSeconds = Math.max(5L, Math.min(lockHeartbeatSeconds, Math.max(5L, lockExpireMinutes * 60 / 3)));

                // 数据库锁心跳（非业务进度心跳）：
                // 周期刷新 lock_time，表示“持锁执行线程仍存活且仍在处理该任务”。
                heartbeatFuture = heartbeatExecutor.scheduleAtFixedRate(() -> {
                    try {
                        Date expectedLockTime = lockTimeRef.get();
                        if (expectedLockTime == null) {
                            return;
                        }
                        Date newLockTime = secondPrecisionDate();
                        int renewed = furnitureVideoTaskMapper.renewTaskLock(task.getId(), expectedLockTime, newLockTime);
                        if (renewed > 0) {
                            lockTimeRef.set(newLockTime);
                        } else {
                            // 未命中通常表示：锁被回收或已被其他实例接管
                            log.warn("任务锁心跳续租未命中, taskId={}, id={}", taskId, task.getId());
                        }
                    } catch (Exception e) {
                        log.error("任务锁心跳续租异常, taskId={}, id={}", taskId, task.getId(), e);
                    }
                }, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);

                // 执行业务：查询并同步外部平台任务进度
                furnitureVideoTaskService.getProcessByTaskId(taskId);
            } catch (Exception e) {
                log.error("同步视频任务进度失败, taskId={}", taskId, e);
            } finally {
                // 任务完成后停止该条任务的心跳续租
                if (heartbeatFuture != null) {
                    heartbeatFuture.cancel(true);
                }
                Date releaseLockTime = lockTimeRef.get();
                log.info("taskId:{},releaseLockTime:{}", taskId, releaseLockTime);
                if (claimed > 0 && releaseLockTime != null) {
                    try {
                        // 条件释放：必须匹配当前 lock_time，避免误释放“他人已接管”的锁
                        int released = furnitureVideoTaskMapper.releaseTaskById(task.getId(), releaseLockTime);
                        if (released <= 0) {
                            log.warn("释放任务处理锁未命中（可能已过期回收或被其他实例接管）, taskId={}, id={}", taskId, task.getId());
                        }
                    } catch (Exception e) {
                        log.error("释放任务处理锁失败, taskId={}, id={}", taskId, task.getId(), e);
                    }
                }
            }
        }
    }

    /**
     * 容器关闭时回收共享心跳线程池，避免线程泄漏。
     */
    @PreDestroy
    public void destroyHeartbeatExecutor() {
        heartbeatExecutor.shutdownNow();
    }

    /**
     * 统一将时间截断到“秒”精度，避免与数据库 DATETIME(0) 精度不一致导致条件更新/释放未命中。
     */
    private Date secondPrecisionDate() {
        return secondPrecisionDate(System.currentTimeMillis());
    }

    private Date secondPrecisionDate(long epochMillis) {
        return new Date((epochMillis / 1000) * 1000);
    }

    /**
     * 每1分钟同步一次未完成视频任务进度
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
