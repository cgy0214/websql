package com.itboy.controller;

import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DbSourceFactory;
import com.itboy.config.ExamineVersionFactory;
import com.itboy.model.AjaxResult;
import com.itboy.model.SysUser;
import com.itboy.service.LoginService;
import com.itboy.util.IpUtil;
import com.itboy.util.StpUtils;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.base.Captcha;
import com.wf.captcha.utils.CaptchaUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

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
        if (ObjectUtil.isEmpty(userName) || ObjectUtil.isEmpty(password)) {
            return AjaxResult.error("账号或密码不能为空!");
        }
        if (ObjectUtil.isEmpty(code) || !CaptchaUtil.ver(code.trim().toLowerCase(), request)) {
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


    @RequestMapping("userController/updateUsers")
    @ResponseBody
    public AjaxResult updateUsers(SysUser sysUser) {
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

}
