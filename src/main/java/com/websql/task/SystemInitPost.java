package com.websql.task;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.cron.CronUtil;
import com.websql.config.DataSourceFactory;
import com.websql.dao.DetectionRepository;
import com.websql.model.DataSourceModel;
import com.websql.model.SysDetectionModel;
import com.websql.model.SysSetup;
import com.websql.model.TimingVo;
import com.websql.service.DbSourceService;
import com.websql.service.LoginService;
import com.websql.service.SseEmitterService;
import com.websql.service.TimingService;
import com.websql.util.CacheUtils;
import com.websql.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @ClassName : SystemInitPost
 * @Description : 统一管理系统初始化任务
 * @Author rabbit boy_0214@sina.com
 * @Date: 2024/03/25 16:35
 */
@Component
@Slf4j
public class SystemInitPost {

    @Autowired
    private ExamineVersionFactory examineVersionFactory;

    @Autowired
    private TimingService timingService;

    @Autowired
    private ExcelClearFactory excelClearFactory;

    @Autowired
    private DetectionRepository detectionRepository;

    @Autowired
    private DbSourceService dbSourceService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private SseEmitterService sseEmitterService;

    /**
     * 初始化系统调度
     */
    @PostConstruct
    public void initSystemTask() {
        initSystemConfig();
        initVersionCheck();
        initExcelClear();
        initTimingTask();
        initDetectionTask();
        initSseHeartBeat();
        // 启动定时任务调度器
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    /**
     * 初始化系统配置
     */
    private void initSystemConfig() {
        SysSetup sysSetup = getSystemSetup();
        if (sysSetup.getInitDataSource() == 0) {
            log.info("Initializing System...");
            loginService.initSystem();
        }
        if (sysSetup.getInitDataSource() == 1) {
            initDataSource();
        }
    }

    /**
     * 初始化作业任务，需要依赖数据源加载完成后执行。
     */
    private void initTimingTask() {
        List<TimingVo> initList = timingService.queryTimingJobList(null);
        log.info("Successful initialization  {}  timerTask.", initList.size());
        ThreadUtil.execAsync(() -> {
            for (TimingVo timingVo : initList) {
                ScheduleUtils.addTimingTask(timingVo.getExecuteTime(), timingVo.getId(), timingVo.getTimingName());
            }
        });
    }

    /**
     * 加载版本检查
     */
    private void initVersionCheck() {
        examineVersionFactory.run();
    }

    /**
     * 加载需要清理的历史文件
     */
    private void initExcelClear() {
        excelClearFactory.run();
    }

    /**
     * 加载监测任务初始化调度
     */
    private void initDetectionTask() {
        SysDetectionModel param = new SysDetectionModel();
        param.setState("开始监控");
        List<SysDetectionModel> initList = detectionRepository.findAll(Example.of(param));
        log.info("Successful initialization  {}  detection Task.", initList.size());
        ThreadUtil.execAsync(() -> {
            for (SysDetectionModel vo : initList) {
                ScheduleUtils.addDetectionTask(vo.getCron(), vo.getId(), vo.getName());
            }
        });
    }

    /**
     * 加载sse心跳检测
     */
    private void initSseHeartBeat() {
        sseEmitterService.startHeartbeat();
    }

    /**
     * 获取系统设置
     */
    public SysSetup getSystemSetup() {
        SysSetup sysSetup = CacheUtils.get("sys_setup", SysSetup.class);
        if (ObjectUtil.isNull(sysSetup)) {
            sysSetup = dbSourceService.initSysSetup();
            CacheUtils.putNoDue("sys_setup", sysSetup);
        }
        return sysSetup;
    }

    /**
     * 初始化数据源
     */
    public int initDataSource() {
        log.info("Initializing DataSource...");
        List<DataSourceModel> dblist = dbSourceService.reloadDataSourceList();
        for (DataSourceModel model : dblist) {
            if (ObjectUtil.isNotEmpty(model.getDbPassword())) {
                String encrypt = PasswordUtil.decrypt(model.getDbPassword());
                model.setDbPassword(encrypt);
            }
            if (ObjectUtil.isNotEmpty(model.getDbAccount())) {
                String encrypt = PasswordUtil.decrypt(model.getDbAccount());
                model.setDbAccount(encrypt);
            }
        }
        if (dblist.size() > 2) {
            log.info("dataSource Size:{}  Async initDataSource ...", dblist.size());
            ThreadUtil.execAsync(() -> DataSourceFactory.initDataSource(dblist));
        } else {
            DataSourceFactory.initDataSource(dblist);
        }
        return dblist.size();
    }

}
