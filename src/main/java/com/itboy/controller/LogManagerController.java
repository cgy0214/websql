package com.itboy.controller;

import com.itboy.model.AjaxResult;
import com.itboy.model.SysLog;
import com.itboy.model.SysUserLog;
import com.itboy.service.DbSourceService;
import com.itboy.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @ClassName : LogManagerController
 * @Description : 日志管理
 * @Author 超 boy_0214@sina.com
 * @Date: 2023/1/29 10:08
 */
@Controller
@RequestMapping("/logManager")
@Slf4j
public class LogManagerController {

    @Resource
    private DbSourceService dbSourceService;


    @Resource
    private LoginService loginService;

    @RequestMapping("/logPage")
    public String sqlLogPage() {
        return "sqlLogPage";
    }

    @RequestMapping("/userLogPage")
    public String userLogPage() {
        return "userLogPage";
    }

    /**
     * 查询日志列表
     *
     * @param model
     * @return
     */
    @RequestMapping("/getLogList")
    @ResponseBody
    public AjaxResult getLogList(SysLog model) {
        return AjaxResult.success(dbSourceService.getLogList(model));
    }


    @RequestMapping("/getUserLogList")
    @ResponseBody
    public AjaxResult getLogList(SysUserLog model) {
        return AjaxResult.success(loginService.getLogList(model));
    }

    /**
     * 删除日志
     *
     * @param type
     * @return
     */
    @RequestMapping("/deleteLog/{type}")
    @ResponseBody
    public AjaxResult delLog(@PathVariable int type) {
        try {
            if (type == 1) {
                dbSourceService.delSysLog();
            } else if (type == 2) {
                dbSourceService.delUserLog();
            }
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }

}
