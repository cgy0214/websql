package com.websql.service.impl;

import com.websql.service.SseEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-Sent Events (SSE) 通信服务实现类
 * <p>
 * 该类提供了基于SSE的实时通信功能，允许服务器向客户端推送实时消息。
 * 主要功能包括：
 * 1. 管理用户与SSE连接的映射关系
 * 2. 支持单发、群发和广播消息
 * 3. 自动处理连接超时和异常断开
 * 4. 提供心跳检测机制保持连接活跃
 * </p>
 * <p>
 * 特点：
 * - 使用ConcurrentHashMap保证线程安全
 * - 支持连接数统计
 * - 自动清理失效连接
 * - 心跳机制维持长连接
 * </p>
 *
 * @author rabbit boy_0214@sina.com
 * @version 1.0
 * @since 2025/11/05
 * @see SseEmitterService
 * @see SseEmitter
 */
@Service
@Slf4j
public class SseEmitterServiceImpl implements SseEmitterService {

    private final Map<String, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private final AtomicInteger connectionCounter = new AtomicInteger(0);

    private static final long TIMEOUT = 30 * 60 * 1000; // 30分钟

    /**
     * 创建SSE连接
     *
     * @param userId 用户ID
     * @return SseEmitter
     */
    @Override
    public SseEmitter createConnection(String userId) {
        if (userEmitters.containsKey(userId)) {
            closeConnection(userId);
        }
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        userEmitters.put(userId, emitter);
        emitter.onCompletion(() -> {
            userEmitters.remove(userId);
            connectionCounter.decrementAndGet();
            log.debug("SSE连接完成: {}", userId);
        });

        emitter.onTimeout(() -> {
            userEmitters.remove(userId);
            connectionCounter.decrementAndGet();
            log.warn("SSE连接超时: {}", userId);
        });

        emitter.onError((e) -> {
            userEmitters.remove(userId);
            connectionCounter.decrementAndGet();
            log.error("SSE连接错误: {}", userId, e);
        });

        connectionCounter.incrementAndGet();

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("连接成功", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.error("发送连接消息失败: {}", e.getMessage(), e);
        }
        return emitter;
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId  用户ID
     * @param message 消息内容
     * @return 发送是否成功
     */
    @Override
    public boolean sendToUser(String userId, Object message) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            return sendMessage(emitter, message, "message");
        }
        return false;
    }

    /**
     * 向所有用户群发消息
     *
     * @param message 消息内容
     * @return 成功发送的用户数量
     */
    @Override
    public int broadcastToAll(Object message) {
        if (userEmitters.isEmpty()) {
            return 0;
        }
        AtomicInteger successCount = new AtomicInteger(0);
        userEmitters.forEach((userId, emitter) -> {
            boolean success = sendMessage(emitter, message, "broadcast");
            if (success) {
                successCount.incrementAndGet();
            }
        });
        return successCount.get();
    }

    /**
     * 向指定用户列表发送消息
     *
     * @param userIds 用户ID列表
     * @param message 消息内容
     * @return 成功发送的用户数量
     */
    @Override
    public int sendToUsers(java.util.List<String> userIds, Object message) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        AtomicInteger successCount = new AtomicInteger(0);

        userIds.forEach(userId -> {
            if (sendToUser(userId, message)) {
                successCount.incrementAndGet();
            }
        });

        return successCount.get();
    }

    /**
     * 关闭指定用户的连接
     *
     * @param userId 用户ID
     */
    @Override
    public void closeConnection(String userId) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("关闭连接异常: {}", e.getMessage(), e);
            } finally {
                userEmitters.remove(userId);
                connectionCounter.decrementAndGet();
            }
        }
    }

    /**
     * 心跳检测
     */
    public void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!userEmitters.isEmpty()) {
                broadcastToAll("heartbeat");
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 内部消息发送方法
     */
    private boolean sendMessage(SseEmitter emitter, Object message, String eventName) {
        if (emitter == null) {
            return false;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(message, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException e) {
            log.error("发送消息失败,{}", e.getMessage(), e);
            userEmitters.values().remove(emitter);
            connectionCounter.decrementAndGet();
            return false;
        } catch (Exception e) {
            log.error("发送消息失败,{}", e.getMessage(), e);
            return false;
        }
    }
}