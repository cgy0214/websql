package com.itboy.task;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName : ScheduleUtils
 * @Description : 定时任务
 * @Author 超 boy_0214@sina.com
 * @Date: 2024/03/25 15:08
 */
@Slf4j
public class ScheduleUtils {

    private static final Map<Long, String> TASK_MAP = new ConcurrentHashMap<>(0);

    private ScheduleUtils() {
    }

    /**
     * 添加任务
     *
     * @param cron   执行表达式
     * @param task   执行类
     * @param taskId 业务ID
     * @return
     */
    public static void addTask(Long taskId, String cron, Task task) {
        String schedule = CronUtil.schedule(cron, task);
        TASK_MAP.put(taskId, schedule);
    }

    /**
     * 删除任务
     *
     * @param taskId 业务ID
     */
    public static void removeTask(Long taskId) {
        String scheduleId = TASK_MAP.get(taskId);
        if (ObjectUtil.isNotNull(scheduleId)) {
            CronUtil.remove(scheduleId);
        }
    }

    /**
     * 增加一个数据同步任务
     *
     * @param cron  执行时间
     * @param taskId 同步任务id
     * @param taskName 同步任务名称
     */
    public static void addTimingTask(String cron, Long taskId, String taskName) {
        JobExecuteFactory jobExecuteFactory = new JobExecuteFactory(taskId, taskName);
        addTask(taskId, cron, jobExecuteFactory);
    }


}