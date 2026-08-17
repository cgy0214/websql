package com.websql.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.websql.config.AiStreamingResponseHandler;
import com.websql.config.JdbcUtils;
import com.websql.model.DataAnalysisQo;
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
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

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

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private SseEmitterService sseEmitterService;

    @Autowired(required = false)
    private ChatMemoryProvider chatMemoryProvider;

    @Autowired(required = false)
    private AiChatMemoryService aiChatMemoryService;

    private static final String CACHE_NAME = "databaseSchema:";

    /**
     * 数据分析提示词
     */
    private static final String DATA_ANALYSIS_PROMPT = "你是一位专业的数据分析师，请根据提供的数据库表结构信息和查询结果数据，进行简明扼要的数据分析。\n"
            + "要求：\n"
            + "1. 全部使用中文回答，内容必须基于提供的数据，严禁编造、猜测数据，如果数据不足或无法得出结论，请如实说明\n"
            + "2. 结果要精炼简短，整体控制在200字以内，使用短句，不要长篇大论，不要凑字数，不要重复描述数据\n"
            + "3. 优先输出结论：数据概览（数量、最大/最小/平均值）、明显趋势、异常值、1-2条最有价值的业务建议\n"
            + "4. 只分析实际返回的字段数据，无需提及字段不一致、行号字段无意义等无关内容\n"
            + "5. 对重点数据使用HTML标签突出显示，例如<strong>加粗</strong>、<span style='color:#FF4D4F'>红色文字</span>\n"
            + "6. 不要使用Markdown格式和JSON格式，HTML标签中的属性使用单引号";

    /**
     * 数据分析追问提示词
     */
    private static final String DATA_ANALYSIS_FOLLOW_UP_PROMPT = "请基于之前的分析结果和对话上下文，回答用户对数据的追问。\n"
            + "要求：\n"
            + "1. 全部使用中文回答，内容必须基于之前提供的数据，严禁编造、猜测数据，如果数据不足或无法得出结论，请如实说明\n"
            + "2. 回答要精炼简短，控制在150字以内，使用短句直接给出结论\n"
            + "3. 对重点数据使用HTML标签突出显示，例如<strong>加粗</strong>、<span style='color:#FF4D4F'>红色文字</span>\n"
            + "4. 不要使用Markdown格式和JSON格式，HTML标签中的属性使用单引号\n";


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
        if (checkDemoTime()) {
            sseEmitterService.sendToUser(userId, "每日10次体验机会,今日已用完请明天再试哦!");
            sseEmitterService.closeConnection(userId);
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
                    new AiStreamingResponseHandler(emitter, chatMemory, prompt.length())
            );
        } else {
            log.error("请检查是否配置了OpenAI API Key,wiki: https://gitee.com/boy_0214/websql/wikis/pages?sort_id=7676296&doc_id=3405209#-openai-%E6%A8%A1%E5%9E%8B%E9%85%8D%E7%BD%AE");
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

    private boolean checkDemoTime() {
        if (!StpUtil.hasRole("demo-admin")) {
            return false;
        }
        String cache = CacheUtils.get("demo:admin:streamAnswer", String.class);
        if (ObjectUtil.isNotNull(cache) && Integer.parseInt(cache) > 10) {
            return true;
        }
        CacheUtils.put("demo:admin:streamAnswer", String.valueOf(Integer.parseInt(cache == null ? "0" : cache) + 1), 24 * 60 * 60);
        return false;
    }

    @Override
    public String summarizeInstance(String taskName, String instanceStatus, String startTime,
                                    String endTime, String executeResult, String sqlContent, String errorMessage) {
        if (ObjectUtil.isNull(chatLanguageModel)) {
            return "AI服务未配置，请检查AI相关配置";
        }

        if (checkDemoTime()) {
            return "每日10次体验机会,今日已用完请明天再试哦!";
        }

        long duration = 0;
        if (ObjectUtil.isNotEmpty(startTime) && ObjectUtil.isNotEmpty(endTime)) {
            try {
                java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime.replace(" ", "T"));
                java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime.replace(" ", "T"));
                duration = java.time.Duration.between(start, end).toMillis();
            } catch (Exception e) {
                log.warn("解析时间失败", e);
            }
        }

        String durationStr = duration > 0 ? String.format("%.1f", duration / 1000.0) : "未知";

        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下任务实例信息，用一句话简洁总结任务执行情况。\n");
        prompt.append("要求：\n");
        prompt.append("1. 一句话总结，80-100字左右\n");
        prompt.append("2. 包含任务名称、任务状态、执行结果、耗时等信息\n");
        prompt.append("3. 如果成功，格式：【名称】执行成功，[具体结果描述]，耗时约X秒\n");
        prompt.append("4. 如果失败，格式：【名称】执行失败，原因：[错误原因]\n");
        prompt.append("5. 不要使用Markdown格式\n");
        prompt.append("6. 直接返回总结内容，不要有前缀\n");
        prompt.append("\n任务信息：\n");
        prompt.append("- 任务名称：").append(taskName != null ? taskName : "未知").append("\n");
        prompt.append("- 任务状态：").append(instanceStatus != null ? instanceStatus : "未知").append("\n");
        prompt.append("- 执行SQL：").append(sqlContent != null ? sqlContent : "无").append("\n");
        prompt.append("- 执行结果：").append(executeResult != null ? executeResult : "无").append("\n");
        prompt.append("- 错误信息：").append(errorMessage != null ? errorMessage : "无").append("\n");
        prompt.append("- 耗时：").append(durationStr).append("秒\n");

        try {
            UserMessage userMessage = UserMessage.from(prompt.toString());
            dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response =
                    chatLanguageModel.generate(userMessage);

            String result = response.content().text();
            if (ObjectUtil.isNotEmpty(result)) {
                result = result.trim();
                return result;
            }
        } catch (Exception e) {
            log.error("AI总结失败", e);
            return "AI总结失败：" + e.getMessage();
        }
        return "任务[" + taskName + "]" + instanceStatus + "，耗时约" + durationStr + "秒";
    }

    @Override
    public SseEmitter streamDataAnalysis(DataAnalysisQo dataAnalysisQo) {
        String userId = StpUtils.getCurrentUserId();
        SseEmitter emitter = sseEmitterService.createConnection(userId);
        if (checkDemoTime()) {
            sseEmitterService.sendToUser(userId, "每日10次体验机会,今日已用完请明天再试哦!");
            sseEmitterService.closeConnection(userId);
            return emitter;
        }
        if (ObjectUtil.isNull(streamingChatLanguageModel)) {
            log.error("请检查是否配置了OpenAI API Key,wiki: https://gitee.com/boy_0214/websql/wikis/pages?sort_id=7676296&doc_id=3405209#-openai-%E6%A8%A1%E5%9E%8B%E9%85%8D%E7%BD%AE");
            sseEmitterService.sendToUser(userId, "请检查是否配置AI相关参数，请参考LOG Wiki配置！");
            sseEmitterService.closeConnection(userId);
            return emitter;
        }
        try {
            String schema = buildSchema(dataAnalysisQo);
            boolean isFollowUp = ObjectUtil.isNotEmpty(dataAnalysisQo.getQuestion());
            String sessionId = dataAnalysisQo.getSessionId();
            String memoryId = ObjectUtil.isNotEmpty(sessionId) ? userId + ":analysis:" + sessionId : userId;
            log.debug("开始请求AI数据分析>>schema:{},isFollowUp:{},memoryId:{}", schema.length(), isFollowUp, memoryId);
            if (ObjectUtil.isNotNull(chatMemoryProvider)) {
                ChatMemory chatMemory = chatMemoryProvider.get(memoryId);
                if (!isFollowUp) {
                    chatMemory.clear();
                    chatMemory.add(new SystemMessage("以下是数据库结构信息，供你参考:\n" + schema));
                } else {
                    boolean needSchemaInfo = chatMemory.messages().stream()
                            .noneMatch(msg -> msg instanceof SystemMessage &&
                                    ((SystemMessage) msg).text().contains("数据库信息:"));
                    if (needSchemaInfo) {
                        chatMemory.add(new SystemMessage("以下是数据库结构信息，供你参考:\n" + schema));
                    }
                }
                String prompt = isFollowUp
                        ? DATA_ANALYSIS_FOLLOW_UP_PROMPT + dataAnalysisQo.getQuestion()
                        : buildDataAnalysisPrompt(dataAnalysisQo, null);
                chatMemory.add(UserMessage.from(prompt));
                log.debug("开始请求AI数据分析>>tokens:{}", prompt.length());
                streamingChatLanguageModel.generate(
                        chatMemory.messages(),
                        new AiStreamingResponseHandler(emitter, chatMemory, prompt.length())
                );
            } else {
                String prompt = isFollowUp
                        ? DATA_ANALYSIS_FOLLOW_UP_PROMPT + dataAnalysisQo.getQuestion() + "\n数据库表结构信息:\n" + schema
                        : buildDataAnalysisPrompt(dataAnalysisQo, schema);
                log.debug("开始请求AI数据分析>>tokens:{}", prompt.length());
                streamingChatLanguageModel.generate(
                        Collections.singletonList(UserMessage.from(prompt)),
                        new AiStreamingResponseHandler(emitter)
                );
            }
        } catch (Exception e) {
            log.error("数据分析请求失败", e);
            sseEmitterService.sendToUser(userId, "数据分析失败：" + e.getMessage());
            sseEmitterService.closeConnection(userId);
        }
        return emitter;
    }

    /**
     * 构建数据库表结构信息
     *
     * @param dataAnalysisQo 数据分析参数
     * @return 表结构信息字符串
     * @throws SQLException SQL异常
     */
    private String buildSchema(DataAnalysisQo dataAnalysisQo) throws SQLException {
        StringBuilder schemaBuilder = new StringBuilder();
        appendDataBaseSchema(dataAnalysisQo.getDataBaseName(), null, schemaBuilder);
        if (ObjectUtil.isNotEmpty(dataAnalysisQo.getTableNameList())) {
            try (Connection connection = JdbcUtils.getConnections(dataAnalysisQo.getDataBaseName())) {
                DatabaseMetaData metaData = connection.getMetaData();
                for (String tableName : dataAnalysisQo.getTableNameList()) {
                    appendTableSchema(schemaBuilder, metaData, tableName);
                }
            }
        }
        return schemaBuilder.toString();
    }

    /**
     * 构建数据分析提示词
     *
     * @param dataAnalysisQo 数据分析参数
     * @param schema         数据库表结构信息，为null时不拼接到提示词中（由SystemMessage携带）
     * @return 构建好的提示词
     */
    private String buildDataAnalysisPrompt(DataAnalysisQo dataAnalysisQo, String schema) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(DATA_ANALYSIS_PROMPT).append("\n");
        if (ObjectUtil.isNotEmpty(schema)) {
            prompt.append("\n数据库表结构信息:\n").append(schema).append("\n");
        }
        prompt.append("执行的SQL语句: ").append(ObjectUtil.defaultIfNull(dataAnalysisQo.getSql(), "")).append("\n");
        prompt.append("查询结果数据(JSON格式): ").append(ObjectUtil.defaultIfNull(dataAnalysisQo.getSampleData(), "")).append("\n");
        prompt.append("开始分析:");
        return prompt.toString();
    }
}