package com.ruoyi.starhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Mapper
public interface FurnitureVideoTaskMapper extends BaseMapper<FurnitureVideoTaskDO> {

    @Update("update furniture_video_task " +
            "set processing = 1, lock_time = #{now} " +
            "where id = #{id} and is_complete = 0 and processing = 0")
    int claimTaskById(@Param("id") Long id, @Param("now") Date now);

    @Update("update furniture_video_task " +
            "set lock_time = #{newLockTime} " +
            "where id = #{id} and is_complete = 0 and processing = 1 and lock_time = #{expectedLockTime}")
    int renewTaskLock(@Param("id") Long id,
                      @Param("expectedLockTime") Date expectedLockTime,
                      @Param("newLockTime") Date newLockTime);

    @Update("update furniture_video_task " +
            "set processing = 0, lock_time = null " +
            "where id = #{id} and processing = 1 and lock_time = #{expectedLockTime}")
    int releaseTaskById(@Param("id") Long id, @Param("expectedLockTime") Date expectedLockTime);

    @Update("update furniture_video_task " +
            "set processing = 0, lock_time = null " +
            "where is_complete = 0 and processing = 1 and lock_time is not null and lock_time < #{expireTime}")
    int releaseExpiredLocks(@Param("expireTime") Date expireTime);
}
