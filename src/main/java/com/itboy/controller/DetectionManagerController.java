package com.itboy.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.itboy.model.AjaxResult;
import com.itboy.model.SysDetectionModel;
import com.itboy.service.DetectionService;
import com.itboy.task.ScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * @ClassName DetectionManagerController
 * @Description 检测任务管理
 * @Author 超 boy_0214@sina.com
 * @Date 2024-10-12  16:17
 **/
@Controller
@RequestMapping("/detectionManager")
@Slf4j
public class DetectionManagerController {

    @Resource
    private DetectionService detectionService;


    @RequestMapping("/listPage")
    public String listPage() {
        return "detectionListPage";
    }

    @RequestMapping("/addPage")
    public String addPage() {
        return "detectionAddPage";
    }

    @RequestMapping("/configPage")
    public String configPage() {
        return "detectionConfigPage";
    }

    @RequestMapping("/reportPage")
    public ModelAndView reportPage(Long id) {
        ModelAndView modelAndView = new ModelAndView("detectionReportPage");
        modelAndView.addObject("id", id);
        return modelAndView;
    }

    /***
     * 加载执行计划列表
     * @param model
     * @return
     */
    @RequestMapping("/list")
    @ResponseBody
    public AjaxResult list(SysDetectionModel model) {
        return AjaxResult.success(detectionService.list(model));
    }


    /**
     * 新增执行计划
     *
     * @param model
     * @return
     */
    @RequestMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody SysDetectionModel model) {
        if (StpUtil.hasRole("demo-admin")) {
            return AjaxResult.error("抱歉,演示角色不允许创建检测任务!");
        }
        if (ObjectUtil.isEmpty(model.getName())) {
            return AjaxResult.error("检测任务名称不能为空哦!");
        }
        if (ObjectUtil.isEmpty(model.getCron())) {
            return AjaxResult.error("Cron表达式不能为空哦!");
        }
        if (ObjectUtil.isEmpty(model.getSqlContent())) {
            return AjaxResult.error("SQL内容不能为空哦!");
        }
        if (ObjectUtil.isEmpty(model.getDataBaseName())) {
            return AjaxResult.error("数据源不能为空哦!");
        }
        Long id = null;
        try {
            id = detectionService.add(model).getId();
            ScheduleUtils.removeDetectionTask(id);
            ScheduleUtils.addDetectionTask(model.getCron(), id, model.getName());
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            if (ObjectUtil.isNotNull(id)) {
                detectionService.deleteById(id);
            }
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/executeOnce/{type}/{id}")
    @ResponseBody
    public AjaxResult executeOnce(@PathVariable("type") int type, @PathVariable("id") Long id, String cron) {
        try {
            SysDetectionModel models = new SysDetectionModel();
            models.setId(id);
            SysDetectionModel vo = detectionService.list(models).getList().get(0);
            ScheduleUtils.removeDetectionTask(id);
            switch (type) {
                case 1:
                    vo.setState("停止监控");
                    detectionService.updateById(vo);
                    break;
                case 2:
                    ScheduleUtils.addDetectionTask(vo.getCron(), id, vo.getName());
                    vo.setState("开始监控");
                    detectionService.updateById(vo);
                    break;
                case 3:
                    vo.setState("开始监控");
                    vo.setCron(cron);
                    ScheduleUtils.addDetectionTask(vo.getCron(), id, vo.getName());
                    detectionService.updateById(vo);
                    break;
                case 4:
                    detectionService.deleteById(id);
                    break;
                case 5:
                    ScheduleUtils.addDetectionTask(vo.getCron(), id, vo.getName());
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

}
