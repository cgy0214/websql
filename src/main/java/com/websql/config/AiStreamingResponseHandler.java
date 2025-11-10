package com.websql.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * AI流式响应处理器
 * <p>
 * 该类实现了LangChain4j的StreamingResponseHandler接口，用于处理AI模型的流式响应。
 * </p>
 *
 * @author rabbit boy_0214@sina.com
 * @see StreamingResponseHandler
 * @see SseEmitter
 * @since 2025/11/04
 */
@Slf4j
public class AiStreamingResponseHandler implements StreamingResponseHandler<AiMessage> {

    private final SseEmitter emitter;

    private final ChatMemory chatMemory;

    private final StringBuilder buffer = new StringBuilder();

    private final int tokens;

    public AiStreamingResponseHandler(SseEmitter emitter) {
        this.emitter = emitter;
        this.tokens = 0;
        this.chatMemory = null;
    }

    public AiStreamingResponseHandler(SseEmitter emitter, ChatMemory chatMemory, int tokens) {
        this.emitter = emitter;
        this.chatMemory = chatMemory;
        this.tokens = tokens;
    }

    @Override
    public void onNext(String token) {
        try {
            // 检查是否有清除聊天历史的特殊指令
            if (token.contains("[CLEAR_HISTORY]")) {
                if (chatMemory != null) {
                    chatMemory.clear();
                }
                // 发送清除历史的信号给前端
                emitter.send(SseEmitter.event().name("clear_history").data("History cleared"));
                return;
            }

            buffer.append(token);
            if (token.endsWith(" ") || token.endsWith("\n")) {
                emitter.send(SseEmitter.event().data(buffer.toString()));
                buffer.setLength(0);
            }
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    @Override
    public void onComplete(Response<AiMessage> response) {
        StreamingResponseHandler.super.onComplete(response);
        if (buffer.length() > 0) {
            try {
                emitter.send(SseEmitter.event().data(buffer.toString()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 将AI响应添加到聊天记忆中
        if (chatMemory != null) {
            chatMemory.add(response.content());
        }

        emitter.complete();
        log.info("请求大模型结束>>tokens:{},response:{}", tokens, response.content().text().length());
    }

    @Override
    public void onError(Throwable throwable) {
        emitter.completeWithError(throwable);
    }
}