package com.itboy.controller;

import com.itboy.model.AjaxResult;
import com.itboy.model.SysSshDto;
import com.itboy.model.SysSshModel;
import com.itboy.service.SshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName : SshManagerController
 * @Description : ssh功能
 * @Author : 超 boy_0214@sina.com
 * @Date: 2023/8/15 16:42
 */
@Controller
@RequestMapping("/sshManager")
@Slf4j
public class SshManagerController {

    @Autowired
    private SshService sshService;

    @PostMapping("/createSession")
    @ResponseBody
    public AjaxResult createSession(@RequestBody SysSshModel sysSshModel) {
        sshService.createSession(sysSshModel);
        return AjaxResult.success();
    }

    @GetMapping("/closeSession")
    @ResponseBody
    public AjaxResult closeSession(@RequestParam String title) {
        sshService.closeSession(title);
        return AjaxResult.success();
    }

    @PostMapping("/exec")
    @ResponseBody
    public AjaxResult exec(@RequestBody SysSshDto dto) {
        return AjaxResult.success(sshService.exec(dto));
    }

    @PostMapping("/bindPort")
    @ResponseBody
    public AjaxResult bindPort(@RequestBody SysSshDto dto) {
        return AjaxResult.success(sshService.bindPort(dto));
    }

    @PostMapping("/unBindPort")
    @ResponseBody
    public AjaxResult unBindPort(@RequestBody SysSshDto dto) {
        return AjaxResult.success(sshService.unBindPort(dto));
    }

}
