package com.itboy.controller;

import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DbSourceFactory;
import com.itboy.model.*;
import com.itboy.service.LoginService;
import com.itboy.util.EnvBeanUtil;
import com.itboy.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

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


    @RequestMapping("/userRolePage")
    public String userRolePage() {
        return "userRolePage";
    }


    @RequestMapping("/addUserPage")
    public String addUserPage() {
        return "addUserPage";
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
        if (sysUserResult.getList().size() == 0) {
            throw new RuntimeException("没有找到用户信息，请重试!");
        }
        return new ModelAndView("updateUserRolesPage").addObject("user", sysUserResult.getList().get(0));
    }


    @RequestMapping("/sysSetUpPage")
    public ModelAndView sysSetUpPage() {
        ModelAndView mav = new ModelAndView("sysSetUpPage");
        SysSetup sysSetup = dbSourceFactory.getSysSetUp();
        mav.addObject("obj", sysSetup);
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


    @RequestMapping("/updateResetPassword/{userId}/{password}")
    @ResponseBody
    public AjaxResult updateResetPassword(@PathVariable Long userId, @PathVariable String password) {
        if (ObjectUtil.isNull(userId) || ObjectUtil.isEmpty(password)) {
            return AjaxResult.error("必填参数不能为空!");
        }
        return AjaxResult.success(loginService.updateResetPassword(userId, password));
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

}
