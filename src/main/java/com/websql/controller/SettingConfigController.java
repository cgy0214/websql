package com.websql.controller;

import cn.hutool.core.util.ObjectUtil;
import com.websql.model.*;
import com.websql.service.*;
import com.websql.task.ExamineVersionFactory;
import com.websql.task.SystemInitPost;
import com.websql.util.CacheUtils;
import com.websql.util.EnvBeanUtil;
import com.websql.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName : SettingConfigController
 * @Description : 系统管理
 * @Author rabbit boy_0214@sina.com
 * @Date: 2023/1/29 17:24
 */
@Controller
@RequestMapping("/settingManager")
@Slf4j
public class SettingConfigController {


    @Autowired
    private SystemInitPost systemInitPost;

    @Autowired
    private LoginService loginService;

    @Autowired
    private ExamineVersionFactory examineVersionFactory;

    @Autowired
    private TeamSourceService teamSourceService;

    @Autowired
    private DbSourceService dbSourceService;

    @Autowired
    private MessageTemplateService messageTemplateService;

    @Autowired
    private DriverCustomService driverCustomService;

    @Autowired
    private BackupService backupService;


    @RequestMapping("/userRolePage")
    public String userRolePage() {
        return "userRolePage";
    }


    @RequestMapping("/addUserPage")
    public String addUserPage() {
        return "addUserPage";
    }

    @RequestMapping("/addTeamPage")
    public String addTeamPage() {
        return "addTeamPage";
    }


    @RequestMapping("/teamManagerPage")
    public String teamManagerPage() {
        return "teamListPage";
    }

    @RequestMapping("/exportLogPage")
    public String timingAddPage() {
        return "exportLogPage";
    }

    @RequestMapping("/addMessageTemplatePage")
    public String addMessageTemplatePage() {
        return "addMessageTemplatePage";
    }


    @RequestMapping("/showTeamResourcePage/{id}")
    public ModelAndView showTeamResourcePage(@PathVariable Long id) {
        ModelAndView modelAndView = new ModelAndView("showTeamResourcePage");
        modelAndView.addObject("id", id);
        return modelAndView;
    }

    @RequestMapping("/addDriverConfigPage")
    public ModelAndView addDriverConfigPage(@RequestParam(required = false) Long id) {
        ModelAndView modelAndView = new ModelAndView("addDriverConfigPage");
        modelAndView.addObject("object", new SysDriverConfig());
        if (ObjectUtil.isNotNull(id)) {
            List<Map<String, String>> driverConfigListSelect = driverCustomService.findDriverConfigListSelect(String.valueOf(id));
            if (!driverConfigListSelect.isEmpty()) {
                Map<String, String> params = driverConfigListSelect.get(0);
                //兼容老数据模型
                if (!params.containsKey("druidFilterType")) {
                    params.put("druidFilterType", "");
                }
                if (params.containsKey("image")) {
                    params.put("image", "");
                }
                modelAndView.addObject("object", params);
            }
        }
        return modelAndView;
    }


    @RequestMapping("/dataBaseConsolePage")
    public ModelAndView dataBaseConsolePage() {
        ModelAndView modelAndView = new ModelAndView("redirect:/h2-console");
        if (!EnvBeanUtil.getBoolean("spring.h2.console.enabled")) {
            modelAndView.setViewName("main");
            modelAndView.addObject("errorMsg", "已关闭数据库控制台，请查看帮助手册修改开启!");
        }
        return modelAndView;
    }

    @RequestMapping("/druidConsolePage")
    public ModelAndView druidConsolePage() {
        ModelAndView modelAndView = new ModelAndView("redirect:/druid");
        if (!EnvBeanUtil.getBoolean("druid.login.enabled")) {
            modelAndView.setViewName("main");
            modelAndView.addObject("errorMsg", "已关闭连接池控制台，请查看帮助手册修改开启!");
        }
        return modelAndView;
    }

    @RequestMapping("/driverConfigPage")
    public String driverConfigPage() {
        return "sysDriverConfigPage";
    }

    @RequestMapping("/updateUserRolesPage/{id}")
    public ModelAndView updateUserRolesPage(@PathVariable Long id) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(id);
        Result<SysUser> sysUserResult = loginService.selectUserRoleList(sysUser);
        if (sysUserResult.getList().isEmpty()) {
            throw new RuntimeException("没有找到用户信息，请重试!");
        }
        List<TeamResourceModel> resourceModels = teamSourceService.queryTeamResourceById(Collections.singletonList(id), "USER");
        String teams = resourceModels.stream().filter(s -> ObjectUtil.isNotEmpty(s.getTeamId())).map(s -> s.getTeamId().toString()).collect(Collectors.joining(","));
        return new ModelAndView("updateUserRolesPage").addObject("user", sysUserResult.getList().get(0)).addObject("teams", teams);
    }


    @RequestMapping("/sysSetUpPage")
    public ModelAndView sysSetUpPage() {
        ModelAndView mav = new ModelAndView("sysSetUpPage");
        SysSetup sysSetup = systemInitPost.getSystemSetup();
        mav.addObject("obj", sysSetup);
        mav.addObject("version", examineVersionFactory.getVersionModel());
        return mav;
    }

    @RequestMapping("/updateSysSetUp")
    @ResponseBody
    public AjaxResult updateSysSetUp(@RequestBody SysSetup sys) {
        return AjaxResult.success(loginService.updateSysSetUp(sys));
    }


    @RequestMapping("/userRoleList")
    @ResponseBody
    public AjaxResult userRoleList(SysUser sysUser) {
        return AjaxResult.success(loginService.selectUserRoleList(sysUser));
    }

    @RequestMapping("/deleteUserRole/{id}")
    @ResponseBody
    public AjaxResult deleteUserRole(@PathVariable Long id) {
        if (ObjectUtil.isNull(id)) {
            return AjaxResult.error("必填参数不能为空!");
        }
        if (!StpUtils.currentSuperAdmin()) {
            return AjaxResult.error("仅允许超级管理员账号删除用户!");
        }
        if (id == 1L) {
            return AjaxResult.error("超级管理员用户不允许删除!");
        }
        return AjaxResult.success(loginService.deleteUserRole(id));
    }


    @RequestMapping("/updateResetPassword")
    @ResponseBody
    public AjaxResult updateResetPassword(@RequestBody Map<String, String> body) {
        if (ObjectUtil.isNull(body) || ObjectUtil.isEmpty(body) ||
                ObjectUtil.isNull(body.get("userId")) || ObjectUtil.isNull(body.get("password"))) {
            return AjaxResult.error("必填参数不能为空!");
        }
        return AjaxResult.success(loginService.updateResetPassword(Long.valueOf(body.get("userId")), body.get("password")));
    }

    @RequestMapping("/queryRolesSelect")
    @ResponseBody
    public AjaxResult queryRolesSelect() {
        return AjaxResult.success(loginService.queryRolesSelect());
    }

    @RequestMapping("/updateUserRole")
    @ResponseBody
    public AjaxResult updateUserRole(@RequestBody SysUser sysUser) {
        return AjaxResult.success(loginService.updateUserRole(sysUser));
    }

    @RequestMapping("/addUserRoleSource")
    @ResponseBody
    public AjaxResult addUserRoleSource(@RequestBody SysUser sysUser) {
        if (ObjectUtil.isEmpty(sysUser.getName())) {
            return AjaxResult.error("名称不能为空!");
        }
        if (ObjectUtil.isEmpty(sysUser.getUserName())) {
            return AjaxResult.error("登录账号不能为空!");
        }
        if (ObjectUtil.isEmpty(sysUser.getPassword())) {
            return AjaxResult.error("登录密码不能为空!");
        }
        if (ObjectUtil.isEmpty(sysUser.getSysRoleName())) {
            return AjaxResult.error("请选择角色信息！!");
        }
        if (ObjectUtil.isEmpty(sysUser.getSysTeamName())) {
            return AjaxResult.error("请选择团队信息！!");
        }
        return loginService.addUserRoleSource(sysUser);
    }


    @RequestMapping("/driverConfigList")
    @ResponseBody
    public AjaxResult driverConfigList(SysDriverConfig sysDriverConfig) {
        return AjaxResult.success(driverCustomService.selectDriverConfigList(sysDriverConfig));
    }

    @RequestMapping("/deleteDriverConfig/{id}")
    @ResponseBody
    public AjaxResult deleteDriverConfig(@PathVariable Long id) {
        if (ObjectUtil.isNull(id)) {
            return AjaxResult.error("必填参数不能为空!");
        }
        return driverCustomService.deleteDriverConfig(id);
    }

    @RequestMapping("/saveOrUpdateDriverConfig")
    @ResponseBody
    public AjaxResult saveOrUpdateDriverConfig(@RequestBody SysDriverConfig sysDriverConfig) {
        return driverCustomService.saveOrUpdateDriverConfig(sysDriverConfig);
    }

    @RequestMapping("/downloadDriver")
    @ResponseBody
    public AjaxResult downloadDriver(@RequestBody DriverDependencyQo qo) {
        try {
            String result = driverCustomService.downloadDriver(qo);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("失败", e);
            return AjaxResult.error(e.getMessage());
        }
    }


    @RequestMapping("/queryTeamList")
    @ResponseBody
    public AjaxResult queryTeamList(TeamSourceModel teamSourceModel) {
        return AjaxResult.success(teamSourceService.selectTeamList(teamSourceModel));
    }

    @RequestMapping("/addTeamSource")
    @ResponseBody
    public AjaxResult addTeamSource(@RequestBody TeamSourceModel teamSourceModel) {
        if (ObjectUtil.isEmpty(teamSourceModel.getTeamName())) {
            return AjaxResult.error("团队名称不能为空!");
        }
        if (ObjectUtil.isEmpty(teamSourceModel.getUserId())) {
            return AjaxResult.error("团队负责人不能为空!");
        }
        return teamSourceService.addTeamSource(teamSourceModel);
    }

    @RequestMapping("/deleteTeam/{id}")
    @ResponseBody
    public AjaxResult deleteTeam(@PathVariable Long id) {
        if (ObjectUtil.isNull(id)) {
            return AjaxResult.error("必填参数不能为空!");
        }
        return teamSourceService.deleteTeam(id);
    }

    /***
     * 加载人员数据源
     * @return
     */
    @RequestMapping("/queryTeamAllBySelect")
    @ResponseBody
    public Map queryTeamAllBySelect() {
        Map result = new HashMap(2);
        result.put("code", 0);
        result.put("data", teamSourceService.queryTeamAllBySelect());
        return result;
    }


    @RequestMapping("/queryTeamResourceList")
    @ResponseBody
    public AjaxResult queryTeamResourceList(@RequestParam Long id) {
        return AjaxResult.success(teamSourceService.queryTeamResourceList(id));
    }


    @RequestMapping("/reloadDataSourceAll")
    @ResponseBody
    public AjaxResult reloadDataSourceAll() {
        int size = systemInitPost.initDataSource();
        return AjaxResult.success(size);
    }

    @GetMapping("/dataBackups")
    public ResponseEntity dataBackups() {
        return backupService.dataBackups();
    }

    @PostMapping("/dataBackups")
    @ResponseBody
    public AjaxResult dataBackups(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return AjaxResult.error("没有解析出文件!");
        }
        return backupService.uploadBackups(file);
    }

    /**
     * 删除所有数据源
     *
     * @return
     */
    @PostMapping("/deleteDataSourceAll")
    @ResponseBody
    public AjaxResult deleteDataSourceAll() {
        dbSourceService.deleteDataSourceAll();
        return AjaxResult.success();
    }

    /***
     * 加载导出日志列表
     * @param model
     * @return
     */
    @RequestMapping("/exportFilesLogList")
    @ResponseBody
    public AjaxResult exportFilesLogList(SysExportModel model) {
        return AjaxResult.success(dbSourceService.exportFilesLogList(model));
    }

    /***
     * 查询报警配置
     * @param model
     * @return
     */
    @RequestMapping("/messageTemplateList")
    @ResponseBody
    public AjaxResult messageTemplateList(SysMessageTemplateModel model) {
        return AjaxResult.success(messageTemplateService.list(model));
    }

    /**
     * 新增报警配置
     *
     * @param sysMessageTemplateModel
     * @return
     */
    @RequestMapping("/addMessageTemplate")
    @ResponseBody
    public AjaxResult addMessageTemplate(@RequestBody SysMessageTemplateModel sysMessageTemplateModel) {
        if (ObjectUtil.isEmpty(sysMessageTemplateModel.getName())) {
            return AjaxResult.error("名称不能为空!");
        }
        if (ObjectUtil.isEmpty(sysMessageTemplateModel.getUrl())) {
            return AjaxResult.error("URL不能为空!");
        }
        if (ObjectUtil.isEmpty(sysMessageTemplateModel.getContent())) {
            return AjaxResult.error("消息内容不能为空!");
        }
        if (ObjectUtil.isEmpty(sysMessageTemplateModel.getType())) {
            return AjaxResult.error("类型不能为空!");
        }
        return messageTemplateService.addMessageTemplate(sysMessageTemplateModel);
    }

    /**
     * 删除告警配置
     *
     * @param id
     * @return
     */
    @RequestMapping("/deleteMessageTemplate")
    @ResponseBody
    public AjaxResult deleteMessageTemplate(@RequestParam Long id) {
        if (ObjectUtil.isEmpty(id)) {
            return AjaxResult.error("参数不能为空!");
        }
        return messageTemplateService.deleteMessageTemplate(id);
    }

    /**
     * 测试发送
     *
     * @param sysMessageTemplateModel
     * @return
     */
    @RequestMapping("/testMessageTemplate")
    @ResponseBody
    public AjaxResult testMessageTemplate(@RequestBody SysMessageTemplateModel sysMessageTemplateModel) {
        if (ObjectUtil.isEmpty(sysMessageTemplateModel.getName())) {
            return AjaxResult.error("名称不能为空!");
        }
        if (ObjectUtil.isEmpty(sysMessageTemplateModel.getUrl())) {
            return AjaxResult.error("URL不能为空!");
        }
        if (ObjectUtil.isEmpty(sysMessageTemplateModel.getContent())) {
            return AjaxResult.error("消息内容不能为空!");
        }
        if (ObjectUtil.isEmpty(sysMessageTemplateModel.getType())) {
            return AjaxResult.error("类型不能为空!");
        }
        return messageTemplateService.testMessageTemplate(sysMessageTemplateModel);
    }

    /***
     * 加载告警模板数据源
     * @return
     */
    @RequestMapping("/findMessageTemplateList")
    @ResponseBody
    public Map findMessageTemplateList() {
        Map result = new HashMap(2);
        result.put("code", 0);
        result.put("data", messageTemplateService.findMessageTemplateList());
        return result;
    }


    @RequestMapping("/clearCache")
    @ResponseBody
    public AjaxResult clearCache() {
        CacheUtils.removeAll();
        CacheUtils.putNoDue("sys_setup", dbSourceService.initSysSetup());
        return AjaxResult.success();
    }

}
