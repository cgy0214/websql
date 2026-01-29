package com.websql.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.websql.model.AjaxResult;
import com.websql.model.BigDataInstanceModel;
import com.websql.model.BigDataTaskModel;
import com.websql.service.BigDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

@Controller
@RequestMapping("/bigdataManager")
@Slf4j
public class BigDataManagerController {

    @Resource
    private BigDataService bigDataService;

    @RequestMapping("/dataDevPage")
    @SaCheckRole("bigdata-admin")
    public ModelAndView dataDevPage(Long id) {
        ModelAndView modelAndView = new ModelAndView("bigdataDataDevPage");
        modelAndView.addObject("id",id);
        return modelAndView;
    }

    @RequestMapping("/taskManagePage")
    @SaCheckRole("bigdata-admin")
    public String taskManagePage() {
        return "bigdataTaskManagePage";
    }

    @RequestMapping("/taskInstancePage")
    @SaCheckRole("bigdata-admin")
    public String taskInstancePage() {
        return "bigdataTaskInstancePage";
    }

    @RequestMapping("/taskList")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public AjaxResult taskList(BigDataTaskModel model) {
        return AjaxResult.success(bigDataService.queryTaskList(model));
    }

    @RequestMapping("/saveTask")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public AjaxResult saveTask(@RequestBody BigDataTaskModel model) {
        try {
            bigDataService.saveTask(model);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("保存任务失败:{}", e.getMessage(), e);
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/deleteTask/{id}")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public AjaxResult deleteTask(@PathVariable Long id) {
        try {
            bigDataService.deleteTask(id);
            return AjaxResult.success("删除成功!");
        } catch (Exception e) {
            log.error("删除任务失败:{}", e.getMessage(), e);
            return AjaxResult.error("删除失败:" + e.getMessage());
        }
    }

    @RequestMapping("/getTask/{id}")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public AjaxResult getTask(@PathVariable Long id) {
        try {
            return AjaxResult.success(bigDataService.getTaskById(id));
        } catch (Exception e) {
            log.error("获取任务失败:{}", e.getMessage(), e);
            return AjaxResult.error("获取失败:" + e.getMessage());
        }
    }

    @RequestMapping("/instanceList")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public AjaxResult instanceList(BigDataInstanceModel model) {
        return AjaxResult.success(bigDataService.queryInstanceList(model));
    }

    @RequestMapping("/saveInstance")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public AjaxResult saveInstance(@RequestBody BigDataInstanceModel model) {
        try {
            bigDataService.saveInstance(model);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("保存实例失败:{}", e.getMessage(), e);
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/deleteInstance/{id}")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public AjaxResult deleteInstance(@PathVariable Long id) {
        try {
            bigDataService.deleteInstance(id);
            return AjaxResult.success("删除成功!");
        } catch (Exception e) {
            log.error("删除实例失败:{}", e.getMessage(), e);
            return AjaxResult.error("删除失败:" + e.getMessage());
        }
    }
}
