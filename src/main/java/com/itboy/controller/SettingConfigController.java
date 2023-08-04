package com.itboy.controller;

import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DbSourceFactory;
import com.itboy.model.AjaxResult;
import com.itboy.model.Result;
import com.itboy.model.SysSetup;
import com.itboy.model.SysUser;
import com.itboy.service.LoginService;
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
    public String dataBaseConsolePage() {
        return "redirect:/h2-console";
    }

    @RequestMapping("/druidConsolePage")
    public String druidConsolePage() {
        return "redirect:/druid";
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

}
