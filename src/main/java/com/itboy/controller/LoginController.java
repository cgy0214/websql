package com.itboy.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DbSourceFactory;
import com.itboy.config.ExamineVersionFactory;
import com.itboy.model.AjaxResult;
import com.itboy.model.SysSetup;
import com.itboy.model.SysUser;
import com.itboy.model.TeamSourceModel;
import com.itboy.service.LoginService;
import com.itboy.util.CheckLoginUserResetUtils;
import com.itboy.util.EnvBeanUtil;
import com.itboy.util.IpUtil;
import com.itboy.util.StpUtils;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.base.Captcha;
import com.wf.captcha.utils.CaptchaUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @ClassName LoginController
 * @Description 登录控制
 * @Author 超 boy_0214@sina.com
 * @Date 2019/6/14 0014 10:51
 **/
@Controller
@Log4j2
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private DbSourceFactory dbSourceFactory;

    @Autowired
    private ExamineVersionFactory examineVersionFactory;

    @RequestMapping(value = {"/", "login", "index"}, method = RequestMethod.GET)
    public ModelAndView loginPage() {
        ModelAndView mav = new ModelAndView("login");
        if (StpUtils.getCurrentLogin()) {
            mav.setViewName("index");
            mav.addObject("users", StpUtils.getCurrentUser());
            mav.addObject("siteConfig", dbSourceFactory.getSysSetUp());
        }
        return mav;
    }

    @RequestMapping("main")
    public String main() {
        return "main";
    }


    @RequestMapping("/getHelpPage")
    public String getHelpPage() {
        return "helpPage";
    }


    @RequestMapping("/getUsersPage")
    public ModelAndView usersPage() {
        ModelAndView mav = new ModelAndView("usersPage");
        mav.addObject("users", StpUtils.getCurrentUser());
        return mav;
    }

    @RequestMapping("/login/captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
        GifCaptcha gifCaptcha = new GifCaptcha(100, 48, 4);
        gifCaptcha.setCharType(Captcha.TYPE_DEFAULT);
        CaptchaUtil.out(gifCaptcha, request, response);
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult login(@RequestBody Map<String, String> map, HttpServletRequest request) throws Exception {
        String userName = map.get("userName");
        String password = map.get("password");
        String code = map.get("captcha");
        Boolean loginEnabled = EnvBeanUtil.getBoolean("login-enabled");
        if (!loginEnabled) {
            return AjaxResult.error("系统已关闭登录入口,请联系管理员!");
        }
        if (ObjectUtil.isEmpty(userName) || ObjectUtil.isEmpty(password)) {
            return AjaxResult.error("账号或密码不能为空!");
        }
        Boolean captchaEnabled = EnvBeanUtil.getBoolean("login-captcha-enabled");
        if (captchaEnabled && (ObjectUtil.isEmpty(code) || !CaptchaUtil.ver(code.trim().toLowerCase(), request))) {
            CaptchaUtil.clear(request);
            return AjaxResult.error("验证码不正确!");
        }
        String ip = IpUtil.getIpAddress(request);
        return loginService.login(userName.trim().toLowerCase(), password.trim().toLowerCase(), ip);
    }

    @RequestMapping("/logout")
    public String logOut() {
        StpUtils.currentLogout();
        return "redirect:/login";
    }


    @RequestMapping("/userController/updateUsers")
    @ResponseBody
    public AjaxResult updateUsers(@RequestBody SysUser sysUser) {
        if (StpUtil.hasRole("demo-admin")) {
            return AjaxResult.error("抱歉,演示账号不允许修改个人信息!");
        }
        return AjaxResult.success(loginService.updateUsers(sysUser));
    }


    /**
     * 查询系统版本
     *
     * @return
     */
    @RequestMapping("queryVersion")
    @ResponseBody
    public AjaxResult queryVersion() {
        return AjaxResult.success(examineVersionFactory.getVersionModel());
    }


    /**
     * 解锁登录
     *
     * @return
     */
    @RequestMapping("unlock")
    @ResponseBody
    public AjaxResult unlock(@RequestParam String pass) {
        if (ObjectUtil.isEmpty(pass)) {
            return AjaxResult.error("密码为空!");
        }
        return loginService.unlock(pass);
    }


    /**
     * 解锁登录
     *
     * @return
     */
    @RequestMapping("unlockLoginUser")
    @ResponseBody
    public AjaxResult unlockLoginUser(@RequestParam(required = false) String code) {
        if (ObjectUtil.isEmpty(code)) {
            log.info("==========================生成重置验证码开始==================================");
            log.info(CheckLoginUserResetUtils.get());
            log.info("===========================用验证码重新请求此接口,重置admin账户登录密码为此验证码！！！=================================");
            return AjaxResult.error("验证码生成完成,请查看日志!");
        }
        if (!CheckLoginUserResetUtils.check(code)) {
            return AjaxResult.error("验证码错误,请重新生成!");
        }
        return loginService.unlockLoginUser(code);
    }

    @ResponseBody
    @RequestMapping(value = "/getSiteConfig", method = RequestMethod.POST)
    public AjaxResult getSiteConfig() {
        SysSetup sysSetUp = dbSourceFactory.getSysSetUp();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("enabledNotification", sysSetUp.getEnabledNotification());
        return AjaxResult.success(resultMap);
    }

    /***
     * 加载人员数据源
     * @return
     */
    @RequestMapping("/queryUsersAllBySelect")
    @ResponseBody
    public Map queryUsersAllBySelect() {
        Map result = new HashMap(2);
        result.put("code", 0);
        result.put("data", loginService.queryUsersAllBySelect());
        return result;
    }

    /***
     * 当前用户的团队信息
     * @return
     */
    @RequestMapping("/queryUserTeams")
    @ResponseBody
    public Map queryUserTeams() {
        List<TeamSourceModel> teamList = StpUtils.getTeamList();
        List<Map<String, String>> resultList = new ArrayList<>(teamList.size());
        if (!teamList.isEmpty()) {
            for (TeamSourceModel team : teamList) {
                Map<String, String> item = new HashMap<>(3);
                item.put("code", team.getId().toString());
                item.put("value", team.getTeamName());
                item.put("select", "false");
                if (ObjectUtil.isNotEmpty(StpUtils.getCurrentActiveTeam())) {
                    if (ObjectUtil.equal(Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId(), team.getId())) {
                        item.put("select", "true");
                    }
                }
                resultList.add(item);
            }
        }
        Map<String, Object> result = new HashMap<>(2);
        result.put("code", 0);
        result.put("data", resultList);
        return result;
    }

    /**
     * 当前用户切换团队信息
     *
     * @return
     */
    @RequestMapping("/updateUserActiveTeam/{teamId}")
    @ResponseBody
    public AjaxResult updateUserTeamActive(@PathVariable Long teamId) {
        if (ObjectUtil.isNull(teamId)) {
            return AjaxResult.error("必填参数为空!");
        }
        List<TeamSourceModel> teamList = StpUtils.getTeamList();
        if (!teamList.isEmpty()) {
            for (TeamSourceModel team : teamList) {
                if (ObjectUtil.equal(team.getId(), teamId)) {
                    StpUtil.getSession().set(StpUtils.SESSION_TEAM_ACTIVE_KEY, team);
                    return AjaxResult.success();
                }
            }
        }
        return AjaxResult.error();
    }

}
