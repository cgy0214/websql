package com.websql.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.websql.config.AiStreamingResponseHandler;
import com.websql.config.JdbcUtils;
import com.websql.model.DataSourceMeta;
import com.websql.service.AiChatMemoryService;
import com.websql.service.SseEmitterService;
import com.websql.service.Text2SqlAdvancedService;
import com.websql.util.CacheUtils;
import com.websql.util.StpUtils;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ClassName  Text2SqlAdvancedServiceImpl
 * Description ai智能生成sql实现类
 * Author rabbit boy_0214@sina.com
 * Date 2025/11/04 10:52
 **/
@Slf4j
@Service
public class Text2SqlAdvancedServiceImpl implements Text2SqlAdvancedService {

    @Autowired(required = false)
    private StreamingChatLanguageModel streamingChatLanguageModel;

    @Autowired
    private SseEmitterService sseEmitterService;

    @Autowired(required = false)
    private ChatMemoryProvider chatMemoryProvider;

    @Autowired(required = false)
    private AiChatMemoryService aiChatMemoryService;

    private static final String CACHE_NAME = "databaseSchema:";


    /**
     * 获取数据库表结构信息
     *
     * @param tableName 表名，如果为null则获取所有表结构
     * @return 表结构信息字符串
     */
    private String getDatabaseSchema(String dataBaseName, String tableName) {
        String cacheKey = CACHE_NAME + dataBaseName + (tableName == null ? "" : tableName);
        String cache = CacheUtils.get(cacheKey, String.class);
        if (ObjectUtil.isNotNull(cache)) {
            return cache;
        }
        StringBuilder schemaBuilder = new StringBuilder();
        appendDataBaseSchema(dataBaseName, tableName, schemaBuilder);
        try (Connection connection = JdbcUtils.getConnections(dataBaseName)) {
            DatabaseMetaData metaData = connection.getMetaData();
            schemaBuilder.append("数据表信息:\n");
            if (tableName != null && !tableName.isEmpty()) {
                appendTableSchema(schemaBuilder, metaData, tableName);
            } else {
                try (ResultSet tables = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                    while (tables.next()) {
                        String table = tables.getString("TABLE_NAME");
                        appendTableSchema(schemaBuilder, metaData, table);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取数据库结构信息失败", e);
            schemaBuilder.append("无法获取数据库结构信息: ").append(e.getMessage());
        }
        CacheUtils.put(cacheKey, schemaBuilder.toString(), 30);
        return schemaBuilder.toString();
    }

    private void appendDataBaseSchema(String dataBase, String tableName, StringBuilder schemaBuilder) {
        DataSourceMeta dataSourceMeta = JdbcUtils.getDataSourceMeta(dataBase, tableName);
        schemaBuilder.append("数据库信息:\n");
        schemaBuilder.append(" - 数据库名称: ").append(dataSourceMeta.getProductName()).append("\n");
        schemaBuilder.append(" - 数据库版本: ").append(dataSourceMeta.getProductVersion()).append("\n");
        schemaBuilder.append(" - 数据库驱动: ").append(dataSourceMeta.getDriverName()).append("\n");
        schemaBuilder.append(" - 驱动版本: ").append(dataSourceMeta.getDriverVersion()).append("\n");
        schemaBuilder.append(" - 数据库只读: ").append(dataSourceMeta.getReadOnly()).append("\n");
        schemaBuilder.append(" - 支持事务: ").append(dataSourceMeta.getSupportsTransactions()).append("\n");
        schemaBuilder.append("\n");
    }

    /**
     * 添加单个表的结构信息
     *
     * @param schemaBuilder 结构信息构建器
     * @param metaData      数据库元数据
     * @param tableName     表名
     * @throws SQLException SQL异常
     */
    private void appendTableSchema(StringBuilder schemaBuilder, DatabaseMetaData metaData, String tableName) throws SQLException {
        schemaBuilder.append("表名: ").append(tableName).append("\n");
        schemaBuilder.append("字段信息:\n");
        try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                String size = columns.getString("COLUMN_SIZE");
                String nullable = columns.getString("IS_NULLABLE");
                schemaBuilder.append("  - ").append(columnName)
                        .append(" (类型: ").append(dataType)
                        .append(", 大小: ").append(size)
                        .append(", 可空: ").append(nullable)
                        .append(")\n");
            }
        }
        schemaBuilder.append("\n");
    }

    /**
     * 构建提示词
     *
     * @param text 用户输入的自然语言
     * @return 构建好的提示词
     */
    private String buildPrompt(String text) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个数据库专家，能够根据用户需求生成准确的SQL查询语句。\n");
        prompt.append("请根据用户需求生成一条SQL查询语句。\n");
        prompt.append("要求:\n");
        prompt.append("1. 只返回SQL语句，不要包含其他解释\n");
        prompt.append("2. 不要使用Markdown格式\n");
        prompt.append("3. 确保语法正确\n");
        prompt.append("4. 尽量使用明确的字段名，避免使用 *\n");
        prompt.append("5. 如果涉及到时间，请使用数据库对应的时间函数\n");
        prompt.append("6. 返回的TOKEN不要粘连注意空格换行\n");
        prompt.append("7. 返回语句要格式化好，不要出现连带\n");
        prompt.append("9. 语句中INSERT INTO,UPDATE,SET,DELETE FROM,SELECT,WHERE,ORDER BY,GROUP BY,HAVING关键词前后要换2次行 \n");
        prompt.append("10. 如果用户想要清除历史记录，请在响应中包含 [CLEAR_HISTORY] 标记\n");
        prompt.append("11. 每个主要关键字换行,使用适当的缩进 \n");
        prompt.append("用户需求: ").append(text).append("\n");
        prompt.append("SQL语句:");
        return prompt.toString();
    }


    @Override
    public SseEmitter streamAnswer(String databaseName, String tableName, String text) {
        String schema = getDatabaseSchema(databaseName, tableName);
        String prompt = buildPrompt(text);
        String userId = StpUtils.getCurrentUserId();
        SseEmitter emitter = sseEmitterService.createConnection(userId);
        if (!checkDemoTime(userId)) {
            return emitter;
        }
        if (ObjectUtil.isNotNull(streamingChatLanguageModel)) {
            ChatMemory chatMemory = chatMemoryProvider.get(userId);

            boolean needSchemaInfo = chatMemory.messages().stream()
                    .noneMatch(msg -> msg instanceof SystemMessage &&
                            ((SystemMessage) msg).text().contains("数据库信息:"));

            if (needSchemaInfo) {
                chatMemory.add(new SystemMessage("以下是数据库结构信息，供你参考:\n" + schema));
            }

            chatMemory.add(UserMessage.from(prompt));

            log.debug("开始请求AI>>tokens:{}", prompt.length());
            streamingChatLanguageModel.generate(
                    chatMemory.messages(),
                    new AiStreamingResponseHandler(emitter, chatMemory,prompt.length())
            );
        } else {
            log.error("请检查是否配置了OpenAI API Key,wiki: https://gitee.com/boy_0214/websql/wikis/%E5%BC%80%E5%8F%91%E6%89%8B%E5%86%8C");
            sseEmitterService.sendToUser(userId, "请检查是否配置AI相关参数，请参考LOG Wiki配置！");
            sseEmitterService.closeConnection(userId);
        }
        return emitter;
    }

    /**
     * 清除指定用户的聊天历史
     *
     * @param userId 用户ID
     */
    @Override
    public void clearUserChatHistory(String userId) {
        aiChatMemoryService.clearUserChatHistory(userId);
    }

    /**
     * 清除当前用户的聊天历史
     */
    @Override
    public void clearCurrentUserChatHistory() {
        String userId = StpUtils.getCurrentUserId();
        clearUserChatHistory(userId);
    }

    private boolean checkDemoTime(String userId) {
        if (!StpUtil.hasRole("demo-admin")) {
            return true;
        }
        String cache = CacheUtils.get("demo:admin:streamAnswer", String.class);
        if (ObjectUtil.isNotNull(cache) && Integer.parseInt(cache) > 1) {
            sseEmitterService.sendToUser(userId, "每日10次体验机会,今日已用完请明天再试哦!");
            sseEmitterService.closeConnection(userId);
            return false;
        }
        CacheUtils.put("demo:admin:streamAnswer", String.valueOf(Integer.parseInt(cache == null ? "0" : cache) + 1), 24 * 60 * 60);
        return true;
    }
}