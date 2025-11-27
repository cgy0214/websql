package com.websql.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
/**
 * AI文本转SQL服务接口
 * <p>
 * 该接口定义了将自然语言转换为SQL语句的高级AI服务功能。
 * 通过流式响应方式实时返回AI生成的SQL语句结果。
 * </p>
 *
 * @author rabbit boy_0214@sina.com
 * @since 2025/11/04
 * @see SseEmitter
 */

public interface Text2SqlAdvancedService {

    SseEmitter streamAnswer(String databaseName,String table,String text);
    
    /**
     * 清除指定用户的聊天历史
     *
     * @param userId 用户ID
     */
    void clearUserChatHistory(String userId);
    
    /**
     * 清除当前用户的聊天历史
     */
    void clearCurrentUserChatHistory();
}