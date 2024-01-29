package com.itboy.config;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.itboy.model.JobLogs;
import com.itboy.model.TimingVo;
import com.itboy.service.TimingService;
import com.itboy.util.ScheduleUtils;
import com.itboy.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @program: JobExecuteFactory
 * @description: 定时任务执行类
 * @author: 超 boy_0214@sina.com
 * @create: 2019-09-17 15:58
 **/
@Slf4j
@Component
public class JobExecuteFactory {


    /**
     * 执行作业
     *
     * @param jobName
     */
    public synchronized void executeSql(String jobName) {
        Long begin = System.currentTimeMillis();
        log.info("{}-作业任务开始.", jobName);
        TimingVo models = new TimingVo();
        models.setTitle(jobName);
        TimingService timingService = SpringContextHolder.getBean(TimingService.class);
        List<TimingVo> voList = timingService.timingList(models).getList();
        if (voList.isEmpty()) {
            log.error("任务已不存在了" + jobName);
            ScheduleUtils.Job job = new ScheduleUtils.Job();
            job.setJobName(jobName);
            ScheduleUtils.cancel(job);
            return;
        }
        StringBuffer param = new StringBuffer();
        StringBuffer values = new StringBuffer();
        JobLogs logs = new JobLogs();
        TimingVo vo = voList.get(0);
        vo.setState("作业中");
        timingService.updateStatus(vo);
        try {
            String sql = vo.getSqlText();
            if (ObjectUtil.isEmpty(sql)) {
                throw new NullPointerException("作业SQL为空");
            }
            //SQL预处理
            Map<String, Object> sqlParser = SqlDruidParser.sqlParser(vo.getTimingName(), sql);
            List<String> executeSqlList = (List<String>) sqlParser.get("executeSql");
            //sync
            if (!ObjectUtil.isEmpty(vo.getSyncName()) && !"请选择".equals(vo.getSyncName())) {
                for (String executeSql : executeSqlList) {
                    Map<String, Object> resultData = JdbcUtils.findMoreResult(vo.getTimingName(), executeSql, new ArrayList<>());
                    if ("2".equals(resultData.get("code"))) {
                        throw new NullPointerException("作业SQL执行异常.error:" + resultData.get("msg"));
                    }
                    List itemList = (List) resultData.get("data");
                    if (itemList.size() > 0) {
                        JSONObject jo = (JSONObject) itemList.get(0);
                        Iterator<String> keys = jo.keySet().iterator();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            param.append("," + key);
                            values.append(",?");
                        }
                        param = param.deleteCharAt(0);
                        values = values.deleteCharAt(0);
                        String tableName = (String) sqlParser.get("tableName");
                        ;
                        if (!ObjectUtil.isEmpty(vo.getSyncTable())) {
                            tableName = vo.getSyncTable();
                        }
                        String itemSql = "insert into " + tableName + "(" + param + ") values (" + values + ")";
                        Map<String, Object> result = JdbcUtils.updateTimers(vo.getSyncName(), itemSql, itemList, param);
                        logs.setTaskError((String) result.get("msg"));
                        logs.setTaskState(result.get("code") == "1" ? "执行成功" : "执行失败");
                        logs.setTaskContent(itemSql);
                        logs.setTaskValue(itemList.toString());
                    }
                }
            } else {
                for (String executeSql : executeSqlList) {
                    Map<String, Object> stringObjectMap = JdbcUtils.findMoreResult(vo.getTimingName(), executeSql, new ArrayList<>());
                    logs.setTaskError(stringObjectMap.get("msg") + ";执行结果:" + stringObjectMap.get("data").toString());
                    logs.setTaskState(stringObjectMap.get("code") == "1" ? "执行成功" : "执行失败");
                    logs.setTaskContent(executeSql);
                }
            }
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
        }
    }

    /**
     * 初始化作业任务
     */
    @PostConstruct
    void initTask() {
        TimingVo models = new TimingVo();
        models.setState("休眠");
        TimingService timingService = SpringContextHolder.getBean(TimingService.class);
        List<TimingVo> initList = timingService.timingList(models).getList();
        log.info("Successful initialization  {}  timerTask.", initList.size());
        ThreadUtil.execAsync(() -> {
            for (TimingVo timingVo : initList) {
                ScheduleUtils.Job job = new ScheduleUtils.Job();
                job.setCron(timingVo.getExecuteTime());
                job.setJobName(timingVo.getTitle());
                job.setClassName("com.itboy.config.JobExecuteFactory");
                job.setMethodName("executeSql");
                job.setStatus(1);
                try {
                    ScheduleUtils.add(job);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
