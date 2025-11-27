package com.websql.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
/**
 * SSE通信服务接口
 * <p>
 * 该接口定义了基于Server-Sent Events (SSE)的实时通信服务规范。
 * 提供了创建连接、发送消息、广播消息以及管理连接等核心功能。
 * </p>
 * @author rabbit boy_0214@sina.com
 * @version 1.0
 * @since 2025/11/05
 * @see SseEmitter
 */

public interface SseEmitterService {

    public SseEmitter createConnection(String userId);

    public boolean sendToUser(String userId, Object message);

    public int broadcastToAll(Object message);

    public int sendToUsers(java.util.List<String> userIds, Object message);

    public void closeConnection(String userId);

    public void startHeartbeat();

}
