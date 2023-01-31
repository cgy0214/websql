package com.itboy.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.itboy.model.AjaxResult;
import com.itboy.util.StpUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * @ClassName : GlobalExceptionHandler
 * @Description : 全局异常处理
 * @Author 超 boy_0214@sina.com
 * @Date: 2023/1/28 23:42
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Object handlerException(HttpServletRequest request, NotLoginException e) {
        return hintErrorMsg(request, e, "您还未登录!");
    }

    @ExceptionHandler(NotPermissionException.class)
    public Object handlerException(HttpServletRequest request, NotPermissionException e) {
        if (isAjax(request)) {
            return AjaxResult.error("缺少权限：" + e.getPermission(), e.getMessage());
        } else {
            ModelAndView modelAndView = new ModelAndView("main");
            modelAndView.addObject("errorMsg", "您没有[" + e.getPermission() + "]权限不允许访问此功能，请联系管理员开通!");
            return modelAndView;
        }
    }

    @ExceptionHandler(NotRoleException.class)
    public Object handlerException(HttpServletRequest request, NotRoleException e) {
        if (isAjax(request)) {
            return AjaxResult.error("缺少角色：" + e.getRole(), e.getMessage());
        } else {
            ModelAndView modelAndView = new ModelAndView("main");
            modelAndView.addObject("errorMsg", "您没有[" + e.getRole() + "]角色不允许访问此功能，请联系管理员开通!");
            return modelAndView;
        }
    }


    @ExceptionHandler(Exception.class)
    public Object handlerException(HttpServletRequest request, Exception e) {
        e.printStackTrace();
        return hintErrorMsg(request, e, "未知异常!");
    }

    public Object hintErrorMsg(HttpServletRequest request, Exception e, String msg) {
        if (isAjax(request)) {
            return AjaxResult.error(msg, e.getMessage());
        } else {
            ModelAndView modelAndView = new ModelAndView("index");
            if (!StpUtils.getCurrentLogin()) {
                modelAndView.setViewName("login");
            }
            return modelAndView;
        }
    }

    public static boolean isAjax(HttpServletRequest request) {
        return (request.getHeader("X-Requested-With") != null && "XMLHttpRequest".equals(request.getHeader("X-Requested-With").toString()));
    }

}
