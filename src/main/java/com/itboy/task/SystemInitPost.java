package com.itboy.task;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.CronUtil;
import com.itboy.config.DbSourceFactory;
import com.itboy.model.TimingVo;
import com.itboy.service.TimingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @ClassName : SystemInitPost
 * @Description : 统一管理系统初始化任务
 * @Author 超 boy_0214@sina.com
 * @Date: 2024/03/25 16:35
 */
@Component
@Slf4j
public class SystemInitPost {

    @Autowired
    private DbSourceFactory dbSourceFactory;

    @Autowired
    private ExamineVersionFactory examineVersionFactory;

    @Autowired
    private TimingService timingService;


    /**
     * 初始化作业任务，需要依赖数据源加载完成后执行。
     */
    void initTask() {
        List<TimingVo> initList = timingService.queryTimingJobList(null);
        log.info("Successful initialization  {}  timerTask.", initList.size());
        ThreadUtil.execAsync(() -> {
            for (TimingVo timingVo : initList) {
                ScheduleUtils.addTimingTask(timingVo.getExecuteTime(), timingVo.getId(), timingVo.getTimingName());
            }
        });
    }

    /**
     * 加载定时任务
     */
    @PostConstruct
    void initCron() {
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    /**
     * 加载数据源
     */
    @PostConstruct
    void initDataSource() {
        dbSourceFactory.initSystem();
        initTask();
    }

    /**
     * 加载版本检查
     */
    @PostConstruct
    void examineVersionFactory() {
        examineVersionFactory.run();
    }

}
