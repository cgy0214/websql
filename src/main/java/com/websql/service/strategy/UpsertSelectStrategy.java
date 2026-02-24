package com.websql.service.strategy;

import cn.hutool.core.util.StrUtil;
import com.websql.config.DataSourceFactory;
import com.websql.config.JdbcUtils;
import com.websql.model.DataSourceTableMeta;
import com.websql.model.ExecuteResult;
import com.websql.model.ParseResultVo;
import com.websql.model.TargetTableInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UpsertSelectStrategy implements SqlExecutionStrategy {

    @Override
    public void execute(Connection connection, ParseResultVo vo, ExecuteResult resultItem) throws Exception {
        int count = bigDataUpsertOverSelect(connection, vo);
        resultItem.setStatus(ExecuteResult.STATUS_SUCCESS);
        resultItem.setData(count);
        resultItem.setType(ExecuteResult.TYPE_NON_SELECT);
    }

    private int bigDataUpsertOverSelect(Connection connection, ParseResultVo vo) {
        try {
            List<Map<String, Object>> data = JdbcUtils.executeQuery(connection, vo.getSelectSql());
            if (data.isEmpty()) {
                log.debug("查询结果为空，无需更新插入");
                return 0;
            }

            TargetTableInfo targetTableInfo = vo.getTargetTableInfo();
            String schema = targetTableInfo.getSchema();
            String tableName = targetTableInfo.getTableName();
            String sourceKey = DataSourceFactory.getBigDataSourceKeyName(schema);

            String dbType = DataSourceFactory.getDbType(sourceKey);
            if (StrUtil.isBlank(dbType)) {
                throw new RuntimeException("无法识别数据库类型: " + schema);
            }

            List<DataSourceTableMeta> keyMetas = JdbcUtils.getKeyMeta(sourceKey, tableName);
            List<String> pks = keyMetas.stream().map(DataSourceTableMeta::getColumnName).collect(Collectors.toList());

            if (pks.isEmpty()) {
                log.debug("表 {} 没有主键，无法执行Upsert操作，尝试执行普通Insert", tableName);
                String insertWithParamsSql = vo.getInsertWithParamsSql();
                String cleanInsertSql = insertWithParamsSql.replaceAll("(?i)INSERT INTO\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\.([a-zA-Z_][a-zA-Z0-9_]*)", "INSERT INTO $2");

                int size = data.get(0).keySet().size();
                String placeholders = String.join(",", java.util.Collections.nCopies(size, "?"));
                cleanInsertSql += " VALUES (" + placeholders + ")";
                return JdbcUtils.batchInsert(sourceKey, cleanInsertSql, data);
            }

            List<String> columns = new ArrayList<>(data.get(0).keySet());
            String upsertSql = generateUpsertSql(dbType, tableName, columns, pks);

            log.debug("生成的Upsert SQL: {}", upsertSql);
            int insertCount = JdbcUtils.batchInsert(sourceKey, upsertSql, data);
            log.debug("批量更新插入完成，影响 {} 条记录", insertCount);
            return insertCount;

        } catch (Exception e) {
            log.error("SQL Upsert执行异常: {}", e.getMessage(), e);
            throw new RuntimeException("更新插入操作失败: " + e.getMessage(), e);
        }
    }

    private String generateUpsertSql(String dbType, String tableName, List<String> columns, List<String> pks) {
        String type = dbType.toLowerCase();
        StringBuilder sb = new StringBuilder();

        if (type.contains("mysql")) {
            sb.append("INSERT INTO ").append(tableName).append(" (");
            sb.append(String.join(", ", columns));
            sb.append(") VALUES (");
            for (int i = 0; i < columns.size(); i++) {
                sb.append(i == 0 ? "?" : ", ?");
            }
            sb.append(") ON DUPLICATE KEY UPDATE ");

            List<String> updateClause = new ArrayList<>();
            for (String col : columns) {
                if (!pks.contains(col)) {
                    updateClause.add(col + " = VALUES(" + col + ")");
                }
            }

            if (updateClause.isEmpty()) {
                String pk = pks.get(0);
                sb.append(pk).append(" = VALUES(").append(pk).append(")");
            } else {
                sb.append(String.join(", ", updateClause));
            }

        } else if (type.contains("postgresql")) {
            sb.append("INSERT INTO ").append(tableName).append(" (");
            sb.append(String.join(", ", columns));
            sb.append(") VALUES (");
            for (int i = 0; i < columns.size(); i++) {
                sb.append(i == 0 ? "?" : ", ?");
            }
            sb.append(") ON CONFLICT (");
            sb.append(String.join(", ", pks));
            sb.append(") ");

            List<String> updateClause = new ArrayList<>();
            for (String col : columns) {
                if (!pks.contains(col)) {
                    updateClause.add(col + " = EXCLUDED." + col);
                }
            }
            if (updateClause.isEmpty()) {
                sb.append("DO NOTHING");
            } else {
                sb.append("DO UPDATE SET ");
                sb.append(String.join(", ", updateClause));
            }
        } else {
            throw new RuntimeException("当前数据库类型 [" + dbType + "] 暂不支持Upsert自动生成，请联系管理员或使用原生SQL。");
        }

        return sb.toString();
    }
}
