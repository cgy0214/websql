package com.websql.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.util.ObjectUtil;
import com.websql.model.AjaxResult;
import com.websql.model.BigDataInstanceModel;
import com.websql.model.BigDataTaskModel;
import com.websql.service.BigDataService;
import com.websql.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/bigdataManager")
@Slf4j
public class BigDataManagerController {

    @Resource
    private BigDataService bigDataService;

    @RequestMapping("/dataDevPage")
    @SaCheckRole("bigdata-admin")
    public ModelAndView dataDevPage(Long id) {
        String currentUserName = StpUtils.getCurrentUserName();
        ModelAndView modelAndView = new ModelAndView("bigdataDataDevPage");
        modelAndView.addObject("data", new BigDataTaskModel());
        modelAndView.addObject("userName", currentUserName);
        if (ObjectUtil.isNotNull(id)) {
            BigDataTaskModel model = bigDataService.getTaskById(id);
            modelAndView.addObject("data", model);
        }
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
            BigDataTaskModel bigDataTaskModel = bigDataService.saveTask(model);
            return AjaxResult.success(bigDataTaskModel);
        } catch (Exception e) {
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

    @RequestMapping("/findDataList")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public Map findDataList() {
        Map result = new HashMap(2);
        result.put("code", 0);
        result.put("data", bigDataService.findDataList());
        return result;
    }

    @RequestMapping("/saveTaskContent")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public AjaxResult saveTaskContent(@RequestBody BigDataTaskModel model) {
        try {
            BigDataTaskModel bigDataTaskModel = bigDataService.saveTaskContent(model);
            return AjaxResult.success(bigDataTaskModel);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/execute")
    @ResponseBody
    @SaCheckRole("bigdata-admin")
    public AjaxResult execute(@RequestBody BigDataTaskModel model) {
        try {
            return AjaxResult.success(bigDataService.execute(model));
        } catch (Exception e) {
            log.error("执行任务失败:{}", e.getMessage(), e);
            return AjaxResult.error(e.getMessage());
        }
    }
}
