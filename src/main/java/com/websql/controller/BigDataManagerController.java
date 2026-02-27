package com.websql.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpStatus;
import com.alibaba.fastjson.JSON;
import com.websql.model.AjaxResult;
import com.websql.model.BigDataInstanceModel;
import com.websql.model.BigDataTaskModel;
import com.websql.model.BigDataTemplateVo;
import com.websql.service.BigDataService;
import com.websql.task.ScheduleUtils;
import com.websql.util.CronUtils;
import com.websql.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName BigDataManagerController
 * @Description 枢易方舟
 * @Author rabbit boy_0214@sina.com
 * @Date 2024/02/10 10:00
 */
@Controller
@RequestMapping("/bigdataManager")
@Slf4j
public class BigDataManagerController {

    @Resource
    private BigDataService bigDataService;

    @RequestMapping("/dataDevPage")
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
    public String taskManagePage() {
        return "bigdataTaskManagePage";
    }

    @RequestMapping("/taskInstancePage")
    public String taskInstancePage() {
        return "bigdataTaskInstancePage";
    }

    @RequestMapping("/taskList")
    @ResponseBody
    public AjaxResult taskList(BigDataTaskModel model) {
        return AjaxResult.success(bigDataService.queryTaskList(model));
    }

    @RequestMapping("/saveTask")
    @ResponseBody
    public AjaxResult saveTask(@RequestBody BigDataTaskModel model) {
        try {
            if (ObjectUtil.isNotEmpty(model.getCron()) && !CronUtils.isValid(model.getCron())) {
                return AjaxResult.error("cron表达式格式不正确，请检查！");
            }
            BigDataTaskModel bigDataTaskModel = bigDataService.saveTask(model);
            return AjaxResult.success(bigDataTaskModel);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/deleteTask/{id}")
    @ResponseBody
    public AjaxResult deleteTask(@PathVariable Long id) {
        try {
            bigDataService.deleteTask(id);
            return AjaxResult.success("删除成功!");
        } catch (Exception e) {
            log.error("删除任务失败:{}", e.getMessage(), e);
            return AjaxResult.error("删除失败:" + e.getMessage());
        }
    }

    @RequestMapping("/deleteTaskAll")
    @ResponseBody
    public AjaxResult deleteTaskAll() {
        try {
            bigDataService.deleteTaskAll();
            return AjaxResult.success("删除成功!");
        } catch (Exception e) {
            log.error("删除任务失败:{}", e.getMessage(), e);
            return AjaxResult.error("删除失败:" + e.getMessage());
        }
    }

    @RequestMapping("/getTask/{id}")
    @ResponseBody
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
    public AjaxResult instanceList(BigDataInstanceModel model) {
        return AjaxResult.success(bigDataService.queryInstanceList(model));
    }

    @RequestMapping("/saveInstance")
    @ResponseBody
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
    public Map findDataList() {
        Map result = new HashMap(2);
        result.put("code", 0);
        result.put("data", bigDataService.findDataList());
        return result;
    }

    @RequestMapping("/saveTaskContent")
    @ResponseBody
    public AjaxResult saveTaskContent(@RequestBody BigDataTaskModel model) {
        try {
            if (ObjectUtil.isNotEmpty(model.getCron()) && !CronUtils.isValid(model.getCron())) {
                return AjaxResult.error("cron表达式格式不正确，请检查！");
            }
            BigDataTaskModel bigDataTaskModel = bigDataService.saveTaskContent(model);
            return AjaxResult.success(bigDataTaskModel);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/execute")
    @ResponseBody
    public AjaxResult execute(@RequestBody BigDataTaskModel model) {
        try {
            return AjaxResult.success(bigDataService.execute(model));
        } catch (Exception e) {
            log.error("执行任务失败:{}", e.getMessage(), e);
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/release")
    @ResponseBody
    public AjaxResult release(@RequestParam Long id, @RequestParam(required = false) String type) {
        if (StpUtil.hasRole("demo-admin")) {
            return AjaxResult.error("抱歉,演示角色不允许发布任务!");
        }
        try {
            BigDataTaskModel bigDataTaskModel = bigDataService.getTaskById(id);
            if (ObjectUtil.isNull(bigDataTaskModel)) {
                return AjaxResult.error("任务不存在!");
            }
            if (ObjectUtil.isNull(bigDataTaskModel.getCron())) {
                return AjaxResult.error("任务未配置定时表达式!");
            }
            if (!CronUtils.isValid(bigDataTaskModel.getCron())) {
                return AjaxResult.error("cron表达式格式不正确，请检查！");
            }
            ScheduleUtils.removeBigDataTask(id);
            if ("已发布".equals(bigDataTaskModel.getStatus()) && ObjectUtil.notEqual("release", type)) {
                bigDataTaskModel.setStatus("未发布");
            } else {
                bigDataTaskModel.setStatus("已发布");
                ScheduleUtils.addBigDataTask(bigDataTaskModel);
            }
            bigDataService.updateTaskById(bigDataTaskModel);
            return AjaxResult.success("操作成功");
        } catch (Exception e) {
            log.error("操作失败,{}", e.getMessage(), e);
            return AjaxResult.error("操作失败:" + e.getMessage());
        }
    }

    /**
     * 查询数据示例模板
     *
     * @param code 编码
     * @return
     */
    @PostMapping("/queryBigDataTemplateList")
    @ResponseBody
    public AjaxResult queryBigDataTemplateList(@RequestParam(required = false) String code) {
        List<BigDataTemplateVo> list = JSON.parseArray(ResourceUtil.readUtf8Str("bigDataTemplate.json"), BigDataTemplateVo.class);
        if (ObjectUtil.isNull(list)) {
            return AjaxResult.success();
        }
        if (ObjectUtil.isNotEmpty(code)) {
            list = list.stream().filter(item -> item.getCode().equals(code)).collect(Collectors.toList());
        }
        return AjaxResult.success(list);
    }

    @RequestMapping("/createDemoTask")
    @ResponseBody
    public AjaxResult createDemoTask(@RequestParam(required = false) String code) {
        try {
            if (ObjectUtil.isNull(code)) {
                return AjaxResult.error("请选择数据示例!");
            }
            AjaxResult ajaxResult = queryBigDataTemplateList(code);
            if (ObjectUtil.notEqual(ajaxResult.getCode(), HttpStatus.HTTP_OK)) {
                return AjaxResult.error("数据示例不存在!");
            }
            List<BigDataTemplateVo> list = (List<BigDataTemplateVo>) ajaxResult.getData();
            BigDataTemplateVo bigDataTemplateVo = list.get(0);
            String sql = String.join("\n", bigDataTemplateVo.getContent());
            BigDataTaskModel model = new BigDataTaskModel();
            model.setTaskName(bigDataTemplateVo.getTitle() + "-" + DateUtil.now());
            model.setDescription(bigDataTemplateVo.getDescription());
            model.setSqlContent(sql);
            model.setCron("0 0 1 * * ?");
            model.setTaskType("SQL任务");
            model.setStatus("草稿");
            model.setCreateTime(DateUtil.now());
            model.setUpdateTime(DateUtil.now());
            model.setId(null);
            model.setTeamId(StpUtils.getCurrentActiveTeam().getId());
            BigDataTaskModel bigDataTaskModel = bigDataService.saveTask(model);
            return AjaxResult.success(bigDataTaskModel.getId());
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

}
