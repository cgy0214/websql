package com.itboy.controller;

import com.itboy.model.AjaxResult;
import com.itboy.model.JobLogs;
import com.itboy.model.TimingVo;
import com.itboy.service.TimingService;
import com.itboy.util.ScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @program: websql
 * @description: 定时业务
 * @Author 超 boy_0214@sina.com
 * @create: 2019-09-16 18:28
 **/
@Controller
@RequestMapping("/timingManager")
@Slf4j
public class TimingController {

    @Resource
    private TimingService timingService;

    @RequestMapping("/addPage")
    public String timingAddPage() {
        return "timingAddPage";
    }

    @RequestMapping("/listPage")
    public String listPage() {
        return "timingListPage";
    }

    @RequestMapping("/historyPage")
    public String historyPage() {
        return "timingLogPage";
    }

    /**
     * 新增执行计划
     *
     * @param model
     * @return
     */
    @RequestMapping("/addTimingData")
    @ResponseBody
    public AjaxResult addTimingData(@RequestBody TimingVo model) {
        TimingVo timingVo = null;
        try {
            timingVo = timingService.addtimingData(model);
            createJob(model.getExecuteTime(), model.getTitle(), 1);
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            timingService.delTiming(timingVo.getId().toString());
            return AjaxResult.error(e.getMessage());
        }
    }

    /***
     * 加载执行计划列表
     * @param model
     * @return
     */
    @RequestMapping("/timingList")
    @ResponseBody
    public AjaxResult timingList(TimingVo model) {
        return AjaxResult.success(timingService.timingList(model));
    }

    @RequestMapping("/executeOnce/{type}/{id}")
    @ResponseBody
    public AjaxResult executeOnce(@PathVariable("type") int type, @PathVariable("id") Long id, String cron) {
        try {
            TimingVo models = new TimingVo();
            models.setId(id);
            TimingVo vo = timingService.timingList(models).getList().get(0);
            ScheduleUtils.Job job = createJob(vo.getExecuteTime(), vo.getTitle(), 2);
            switch (type) {
                case 1:
                    ScheduleUtils.cancel(job);
                    vo.setState("停用");
                    timingService.updateTiming(vo);
                    break;
                case 2:
                    createJob(vo.getExecuteTime(), vo.getTitle(), 1);
                    vo.setState("休眠");
                    timingService.updateTiming(vo);
                    break;
                case 3:
                    vo.setExecuteTime(cron);
                    timingService.updateTiming(vo);
                    createJob(cron, vo.getTitle(), 1);
                    break;
                case 4:
                    timingService.delTiming(String.valueOf(id));
                    ScheduleUtils.cancel(job);
                    break;
                case 5:
                    createJob(vo.getExecuteTime(), vo.getTitle(), 4);
                    break;
                default:
                    throw new RuntimeException("错误的类型!" + type);
            }
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }

    private ScheduleUtils.Job createJob(String Corn, String JobName, int status) throws Exception {
        ScheduleUtils.Job job2 = new ScheduleUtils.Job();
        job2.setClassName("com.itboy.config.JobExecuteFactory");
        job2.setMethodName("executeSql");
        job2.setCron(Corn);
        job2.setJobName(JobName);
        job2.setStatus(1);
        if (status == 1) {
            ScheduleUtils.cancel(job2);
            ScheduleUtils.add(job2);
        } else if (status == 4) {
            ScheduleUtils.cancel(job2);
            ScheduleUtils.add(job2);
            Thread.sleep(1000);
            ScheduleUtils.cancel(job2);
        }
        return job2;
    }

    /***
     * 加载执行日志列表
     * @param model
     * @return
     */
    @RequestMapping("/jobLogList")
    @ResponseBody
    public AjaxResult jobLogList(JobLogs model) {
        return AjaxResult.success(timingService.jobLogsList(model));
    }


    /**
     * 删除日志
     *
     * @return
     */
    @RequestMapping("/jobLogDelete")
    @ResponseBody
    public AjaxResult jobLogDelete() {
        try {
            timingService.jobLogDelete();
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }
}
