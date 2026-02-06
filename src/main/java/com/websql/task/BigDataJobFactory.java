package com.websql.task;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.cron.task.Task;
import com.alibaba.fastjson.JSON;
import com.websql.model.*;
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
        if (ObjectUtil.isNull(bigDataTaskModel)){
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
        if(ObjectUtil.notEqual("已发布", vo.getStatus())){
            log.debug("{}-枢易方舟任务未发布,终止运行...", bigDataTaskModel.getTaskName());
            return;
        }
        try {
            //todo 创建作业执行实例，校验sql返回结果，发送告警
            List<ExecuteResult> execute = bigDataService.execute(bigDataTaskModel);
            log.info("{}-枢易方舟任务执行结果:{}", bigDataTaskModel.getTaskName(), JSON.toJSONString(execute));
        } catch (Exception e) {
            log.error("{}-枢易方舟任务执行失败,{}", bigDataTaskModel.getTaskName(),e.getMessage(),e);
        } finally {
            Long end = System.currentTimeMillis();
            Long time = (end - begin);
            log.debug("{}-枢易方舟任务完成,time:{}", bigDataTaskModel.getTaskName(), time);
        }
    }


}
