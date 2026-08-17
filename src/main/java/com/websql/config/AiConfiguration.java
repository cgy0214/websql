package com.websql.config;

import cn.hutool.core.util.ObjectUtil;
import com.websql.dao.SysSetUpRepository;
import com.websql.model.SysSetup;
import com.websql.service.AiChatMemoryService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI配置类
 *
 * @author rabbit boy_0214@sina.com
 * @since 2025/11/04
 * @see ChatLanguageModel
 * @see StreamingChatLanguageModel
 * @see ChatMemoryProvider
 */
@Configuration
public class AiConfiguration {

    private static final String RESPONSE_FORMAT = "text";

    @Value("${openai.api.url:}")
    private String propertyUrl;

    @Value("${openai.api.key:}")
    private String propertyApiKey;

    @Value("${openai.api.modelName:}")
    private String propertyModelName;

    @Value("${openai.api.temperature:0.0}")
    private double propertyTemperature;

    @Value("${openai.api.maxTokens:1024}")
    private int propertyMaxTokens;

    @Value("${openai.api.maxMessages:20}")
    private int propertyMaxMessages;

    private final SysSetUpRepository sysSetUpRepository;

    public AiConfiguration(SysSetUpRepository sysSetUpRepository) {
        this.sysSetUpRepository = sysSetUpRepository;
    }

    /**
     * 获取系统设置表中的AI配置，查询失败或表为空时返回null
     */
    private SysSetup getSysSetup() {
        try {
            List<SysSetup> sysList = sysSetUpRepository.findAll();
            if (!sysList.isEmpty()) {
                return sysList.get(0);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getApiKey() {
        SysSetup sysSetup = getSysSetup();
        if (sysSetup != null && ObjectUtil.isNotEmpty(sysSetup.getAiKey())) {
            return sysSetup.getAiKey();
        }
        return propertyApiKey;
    }

    private String getApiUrl() {
        SysSetup sysSetup = getSysSetup();
        if (sysSetup != null && ObjectUtil.isNotEmpty(sysSetup.getAiUrl())) {
            return sysSetup.getAiUrl();
        }
        return propertyUrl;
    }

    private String getModelName() {
        SysSetup sysSetup = getSysSetup();
        if (sysSetup != null && ObjectUtil.isNotEmpty(sysSetup.getAiModelName())) {
            return sysSetup.getAiModelName();
        }
        return propertyModelName;
    }

    private double getTemperature() {
        SysSetup sysSetup = getSysSetup();
        if (sysSetup != null && ObjectUtil.isNotNull(sysSetup.getAiTemperature())) {
            return sysSetup.getAiTemperature();
        }
        return propertyTemperature;
    }

    private int getMaxTokens() {
        SysSetup sysSetup = getSysSetup();
        if (sysSetup != null && ObjectUtil.isNotNull(sysSetup.getAiMaxTokens())) {
            return sysSetup.getAiMaxTokens();
        }
        return propertyMaxTokens;
    }

    private int getMaxMessages() {
        SysSetup sysSetup = getSysSetup();
        if (sysSetup != null && ObjectUtil.isNotNull(sysSetup.getAiMaxMessages())) {
            return sysSetup.getAiMaxMessages();
        }
        return propertyMaxMessages;
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (ObjectUtil.isEmpty(getApiKey())) {
            return null;
        }
        return OpenAiChatModel.builder()
                .apiKey(getApiKey())
                .baseUrl(getApiUrl())
                .modelName(getModelName())
                .temperature(getTemperature())
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        if (ObjectUtil.isEmpty(getApiKey())) {
            return null;
        }
        return OpenAiStreamingChatModel.builder()
                .apiKey(getApiKey())
                .baseUrl(getApiUrl())
                .modelName(getModelName())
                .temperature(getTemperature())
                .maxTokens(getMaxTokens())
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
                        .maxMessages(getMaxMessages())
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
