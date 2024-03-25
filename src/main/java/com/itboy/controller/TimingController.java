package com.itboy.controller;

import cn.hutool.core.util.ObjectUtil;
import com.itboy.model.AjaxResult;
import com.itboy.model.JobLogs;
import com.itboy.model.TimingVo;
import com.itboy.service.TimingService;
import com.itboy.task.ScheduleUtils;
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
        if (ObjectUtil.isEmpty(model.getTitle())) {
            return AjaxResult.error("作业名称不能为空哦!");
        }
        if (ObjectUtil.isEmpty(model.getExecuteTime())) {
            return AjaxResult.error("Cron表达式不能为空哦!");
        }
        if (ObjectUtil.isEmpty(model.getSyncTable())) {
            return AjaxResult.error("目标数据表名不能为空哦!");
        }
        Long id = null;
        try {
            id = timingService.addtimingData(model).getId();
            createJob(model.getExecuteTime(), model.getId(), model.getTitle());
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            if (ObjectUtil.isNotNull(id)) {
                timingService.delTiming(id);
            }
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
            switch (type) {
                case 1:
                    ScheduleUtils.removeTask(id);
                    vo.setState("停用");
                    timingService.updateTiming(vo);
                    break;
                case 2:
                    createJob(vo.getExecuteTime(), vo.getId(), vo.getTitle());
                    vo.setState("休眠");
                    timingService.updateTiming(vo);
                    break;
                case 3:
                    vo.setExecuteTime(cron);
                    timingService.updateTiming(vo);
                    createJob(cron, vo.getId(), vo.getTitle());
                    break;
                case 4:
                    timingService.delTiming(id);
                    ScheduleUtils.removeTask(id);
                    break;
                case 5:
                    createJob(vo.getExecuteTime(), vo.getId(), vo.getTitle());
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

    private void createJob(String cron, Long id, String JobName) throws Exception {
        ScheduleUtils.removeTask(id);
        ScheduleUtils.addTimingTask(cron, id, JobName);
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
