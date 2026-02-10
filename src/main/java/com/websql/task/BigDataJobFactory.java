package com.websql.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.cron.task.Task;
import com.alibaba.fastjson.JSON;
import com.websql.model.BigDataInstanceModel;
import com.websql.model.BigDataTaskModel;
import com.websql.model.ExecuteResult;
import com.websql.service.BigDataService;
import com.websql.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 枢易方舟任务调度
 **/
@Slf4j
public class BigDataJobFactory implements Task {


    private final BigDataTaskModel bigDataTaskModel;


    public BigDataJobFactory(BigDataTaskModel bigDataTaskModel) {
        this.bigDataTaskModel = bigDataTaskModel;
    }

    /**
     * 执行枢易方舟任务
     */
    @Override
    public void execute() {
        if (ObjectUtil.isNull(bigDataTaskModel)) {
            log.debug("枢易方舟作业任务参数错误终止运行...");
            return;
        }
        Long begin = System.currentTimeMillis();
        log.debug("{}-枢易方舟任务开始...", bigDataTaskModel.getTaskName());
        BigDataService bigDataService = SpringContextHolder.getBean(BigDataService.class);
        BigDataTaskModel vo = bigDataService.getTaskById(bigDataTaskModel.getId());
        if (ObjectUtil.isNull(vo)) {
            ScheduleUtils.removeTask(bigDataTaskModel.getId(), "BIGDATA", false);
            return;
        }
        if (ObjectUtil.notEqual("已发布", vo.getStatus())) {
            log.debug("{}-枢易方舟任务未发布,终止运行...", bigDataTaskModel.getTaskName());
            return;
        }
        BigDataInstanceModel instance = new BigDataInstanceModel();
        instance.setTaskId(vo.getId());
        instance.setTaskName(vo.getTaskName());
        instance.setInstanceStatus("运行中");
        instance.setStartTime(DateUtil.now());
        instance.setCreateTime(DateUtil.now());
        instance.setCreateUser("SYSTEM");
        instance.setEndTime(DateUtil.now());
        instance.setTaskCreateUser(vo.getCreateUser());
        instance.setSqlContent(vo.getSqlContent());
        try {
            List<ExecuteResult> execute = bigDataService.execute(vo);
            log.debug("{}-枢易方舟任务执行结果:{}", bigDataTaskModel.getTaskName(), JSON.toJSONString(execute));
            if (ObjectUtil.isNull(execute)) {
                instance.setErrorMessage("执行任务结果为空");
                instance.setInstanceStatus("失败");
                log.warn("{}-枢易方舟任务返回结果为空.", bigDataTaskModel.getTaskName());
                return;
            }
            for (ExecuteResult executeResult : execute) {
                if (ObjectUtil.isNull(executeResult)) {
                    instance.setErrorMessage("执行结果为空");
                    instance.setInstanceStatus("失败");
                    log.warn("{}-枢易方舟任务执行结果为空.", bigDataTaskModel.getTaskName());
                    continue;
                }
                if (ObjectUtil.notEqual(ExecuteResult.STATUS_SUCCESS, executeResult.getStatus())) {
                    instance.setErrorMessage(executeResult.getErrorMessage());
                    instance.setInstanceStatus("失败");
                    log.error("{}-枢易方舟任务执行失败,{}", bigDataTaskModel.getTaskName(), executeResult.getErrorMessage());
                }
            }
            instance.setExecuteResult(JSON.toJSONString(execute));
            instance.setInstanceStatus("成功");
        } catch (Exception e) {
            log.error("{}-枢易方舟任务执行失败,{}", bigDataTaskModel.getTaskName(), e.getMessage(), e);
            instance.setErrorMessage(e.getMessage());
            instance.setInstanceStatus("失败");
        } finally {
            Long end = System.currentTimeMillis();
            Long time = (end - begin);
            instance.setEndTime(DateUtil.now());
            bigDataService.saveInstance(instance);
            log.debug("{}-枢易方舟任务完成,time:{}", bigDataTaskModel.getTaskName(), time);
        }
    }


}
