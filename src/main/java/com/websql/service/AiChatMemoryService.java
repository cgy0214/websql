package com.websql.service;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI聊天记忆服务
 * <p>
 * 提供管理AI聊天历史的功能，包括清除特定用户或所有用户的聊天历史。
 * </p>
 *
 * @author rabbit boy_0214@sina.com
 * @since 2025/11/06
 */
@Service
public class AiChatMemoryService {

    @Autowired(required = false)
    private ChatMemoryProvider chatMemoryProvider;

    /**
     * 清除指定用户的聊天历史
     *
     * @param userId 用户ID
     */
    public void clearUserChatHistory(String userId) {
        if (chatMemoryProvider instanceof AiChatMemoryProvider) {
            ((AiChatMemoryProvider) chatMemoryProvider).clear(userId);
        }
    }

    /**
     * 清除所有用户的聊天历史
     */
    public void clearAllChatHistory() {
        if (chatMemoryProvider instanceof AiChatMemoryProvider) {
            ((AiChatMemoryProvider) chatMemoryProvider).clearAll();
        }
    }

    /**
     * 扩展ChatMemoryProvider接口，添加清除功能
     */
    public interface AiChatMemoryProvider extends ChatMemoryProvider {
        /**
         * 清除指定用户的聊天历史
         * @param memoryId 用户ID
         */
        void clear(Object memoryId);

        /**
         * 清除所有用户的聊天历史
         */
        void clearAll();
    }
}