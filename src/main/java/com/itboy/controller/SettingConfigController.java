package com.itboy.controller;

import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DbSourceFactory;
import com.itboy.model.*;
import com.itboy.service.LoginService;
import com.itboy.service.TeamSourceService;
import com.itboy.task.ExamineVersionFactory;
import com.itboy.util.EnvBeanUtil;
import com.itboy.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName : SettingConfigController
 * @Description : 系统管理
 * @Author 超 boy_0214@sina.com
 * @Date: 2023/1/29 17:24
 */
@Controller
@RequestMapping("/settingManager")
@Slf4j
public class SettingConfigController {


    @Autowired
    private DbSourceFactory dbSourceFactory;


    @Autowired
    private LoginService loginService;

    @Autowired
    private ExamineVersionFactory examineVersionFactory;

    @Autowired
    private TeamSourceService teamSourceService;


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
            List<Map<String, String>> driverConfigListSelect = loginService.findDriverConfigListSelect(String.valueOf(id));
            if (driverConfigListSelect.size() > 0) {
                modelAndView.addObject("object", driverConfigListSelect.get(0));
            }
        }
        return modelAndView;
    }


    @RequestMapping("/dataBaseConsolePage")
    public ModelAndView dataBaseConsolePage() {
        ModelAndView modelAndView = new ModelAndView("redirect:/h2-console");
        if (!EnvBeanUtil.getBoolean("spring.h2.console.enabled")) {
            modelAndView.setViewName("main");
            modelAndView.addObject("errorMsg", "抱歉，已关闭数据库控制台，请联系管理员修改启动配置!");
        }
        return modelAndView;
    }

    @RequestMapping("/druidConsolePage")
    public ModelAndView druidConsolePage() {
        ModelAndView modelAndView = new ModelAndView("redirect:/druid");
        if (!EnvBeanUtil.getBoolean("spring.h2.console.enabled")) {
            modelAndView.setViewName("main");
            modelAndView.addObject("errorMsg", "抱歉，已关闭连接池控制台，请联系管理员修改启动配置!");
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
        SysSetup sysSetup = dbSourceFactory.getSysSetUp();
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
        return AjaxResult.success(loginService.selectdriverConfigList(sysDriverConfig));
    }

    @RequestMapping("/deleteDriverConfig/{id}")
    @ResponseBody
    public AjaxResult deleteDriverConfig(@PathVariable Long id) {
        if (ObjectUtil.isNull(id)) {
            return AjaxResult.error("必填参数不能为空!");
        }
        return loginService.deleteDriverConfig(id);
    }

    @RequestMapping("/saveOrUpdateDriverConfig")
    @ResponseBody
    public AjaxResult saveOrUpdateDriverConfig(@RequestBody SysDriverConfig sysDriverConfig) {
        return AjaxResult.success(loginService.saveOrUpdateDriverConfig(sysDriverConfig));
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
        int size = dbSourceFactory.initDataSource();
        return AjaxResult.success(size);
    }

}
