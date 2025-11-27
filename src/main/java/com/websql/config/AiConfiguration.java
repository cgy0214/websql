package com.websql.config;

import com.websql.service.AiChatMemoryService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI配置类
 * <p>
 * 该配置类用于初始化和配置AI相关组件，包括语言模型、流式语言模型和聊天记忆提供者。
 * </p>
 *
 * @author rabbit boy_0214@sina.com
 * @since 2025/11/04
 * @see ChatLanguageModel
 * @see StreamingChatLanguageModel
 * @see ChatMemoryProvider
 */
@Configuration
@ConditionalOnExpression("'${openai.api.key:}' != ''")
public class AiConfiguration {

    @Value("${openai.api.url}")
    private String url;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.modelName}")
    private String modelName;

    @Value("${openai.api.temperature}")
    private double temperature;

    @Value("${openai.api.maxTokens}")
    private int maxTokens;

    @Value("${openai.api.maxMessages}")
    private int maxMessages;

    private static final String RESPONSE_FORMAT = "text";

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .baseUrl(url)
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(openAiApiKey)
                .baseUrl(url)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .responseFormat(RESPONSE_FORMAT)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        Map<Object, MessageWindowChatMemory> memoryMap = new ConcurrentHashMap<>();
        return new AiChatMemoryService.AiChatMemoryProvider() {
            @Override
            public MessageWindowChatMemory get(Object memoryId) {
                return memoryMap.computeIfAbsent(memoryId, id ->
                    MessageWindowChatMemory.builder()
                        .maxMessages(maxMessages)
                        .id(id)
                        .build()
                );
            }
            /**
             * 清除指定用户的聊天历史
             * @param memoryId 用户ID
             */
            @Override
            public void clear(Object memoryId) {
                MessageWindowChatMemory memory = memoryMap.remove(memoryId);
                if (memory != null) {
                    memory.clear();
                }
            }
            
            /**
             * 清除所有用户的聊天历史
             */
            @Override
            public void clearAll() {
                for (MessageWindowChatMemory memory : memoryMap.values()) {
                    memory.clear();
                }
                memoryMap.clear();
            }
        };
    }
}