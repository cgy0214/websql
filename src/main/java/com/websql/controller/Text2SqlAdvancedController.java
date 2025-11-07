package com.websql.controller;

import com.websql.model.AjaxResult;
import com.websql.service.AiChatMemoryService;
import com.websql.service.Text2SqlAdvancedService;
import com.websql.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI文本转SQL控制器
 * <p>
 * 该控制器处理用户通过自然语言生成SQL语句的请求。
 * 支持实时返回AI生成的SQL语句结果。
 * </p>
 *
 * @author rabbit boy_0214@sina.com
 * @see Text2SqlAdvancedService
 * @since 2025/11/05
 */

@Slf4j
@Controller
@RequestMapping("/aiManager")
public class Text2SqlAdvancedController {

    @Autowired
    private Text2SqlAdvancedService text2SqlAdvancedService;


    @Autowired
    private AiChatMemoryService aiChatMemoryService;

    @RequestMapping("/page")
    public ModelAndView page(@RequestParam(required = false) String database, @RequestParam(required = false) String table) {
        ModelAndView mav = new ModelAndView("aiPage");
        mav.addObject("database", database);
        mav.addObject("table", table);
        return mav;
    }


    /**
     * 根据自然语言和表名自动生成SQL语句
     *
     * @param text 用户输入的自然语言
     * @return 生成的SQL语句
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter streamAnswer(@RequestParam String dataBaseName,
                                   @RequestParam(required = false) String tableName,
                                   @RequestParam(required = false) String text) {
        //todo 是否允许单用户并发请求或上次解答未完成时再次提问
        return text2SqlAdvancedService.streamAnswer(dataBaseName, tableName, text);
    }

    /**
     * 清除当前用户的聊天历史
     *
     * @return 操作结果
     */
    @GetMapping("/clear")
    public AjaxResult clearCurrentUserChatHistory() {
        String userId = StpUtils.getCurrentUserId();
        aiChatMemoryService.clearUserChatHistory(userId);
        return AjaxResult.success("聊天历史已清除");
    }

    /**
     * 清除所有用户的聊天历史（需要管理员权限）
     *
     * @return 操作结果
     */
    @GetMapping("/clearAll")
    public AjaxResult clearAllChatHistory() {
        if (!StpUtils.currentSuperAdmin()) {
            return AjaxResult.error("仅允许超级管理员清空聊天历史!");
        }
        aiChatMemoryService.clearAllChatHistory();
        return AjaxResult.success("所有聊天历史已清除");
    }

}
