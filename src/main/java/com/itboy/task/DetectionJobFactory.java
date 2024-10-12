package com.itboy.task;

import cn.hutool.cron.task.Task;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: DetectionJobFactory
 * @description: 定时检测任务执行类
 * @author: 超 boy_0214@sina.com
 * @create: 2024-10-12 17:20
 **/
@Slf4j
public class DetectionJobFactory implements Task {


    private final Long id;

    private final String jobName;

    public DetectionJobFactory(Long id, String jobName) {
        this.id = id;
        this.jobName = jobName;
    }

    /**
     * 执行作业
     */
    @Override
    public void execute() {
        Long begin = System.currentTimeMillis();
        log.info("{}-作业任务开始.", jobName);
       /* TimingService timingService = SpringContextHolder.getBean(TimingService.class);
        TimingVo vo = timingService.queryTimingJobById(id);
        if (ObjectUtil.isNull(vo)) {
            ScheduleUtils.removeTask(id,"timing",false);
            return;
        }
        vo.setState("作业中");
        timingService.updateStatus(vo);
        StringBuffer param = new StringBuffer();
        StringBuffer values = new StringBuffer();
        JobLogs logs = new JobLogs();
        logs.setTeamId(vo.getTeamId());
        try {
            String sql = Base64Decoder.decodeStr(vo.getSqlText());
            if (ObjectUtil.isEmpty(sql)) {
                throw new NullPointerException("作业SQL为空");
            }
            //SQL预处理
            List<SqlParserVo> parserVoList = SqlParserHandler.getParserVo(vo.getTimingName(), sql);

        } catch (Exception e) {
            e.printStackTrace();
            logs.setTaskState("执行失败");
            logs.setTaskError(e.getMessage());
            log.error("定时作业异常.", e);
        } finally {
            Long end = System.currentTimeMillis();
            Long exDate = end - begin;
            log.info("{}-作业任务结束. {}ms.", jobName, exDate);
            logs.setTaskId(vo.getId());
            logs.setTaskName(vo.getTitle());
            logs.setCo1(String.valueOf(exDate));
            vo.setState("休眠");
            timingService.updateStatus(vo);
            timingService.saveLogs(logs);
        }*/
    }


}
