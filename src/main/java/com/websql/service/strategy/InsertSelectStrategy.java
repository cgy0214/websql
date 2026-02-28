package com.websql.service.strategy;

import com.websql.config.DataSourceFactory;
import com.websql.config.JdbcUtils;
import com.websql.model.ExecuteResult;
import com.websql.model.ParseResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InsertSelectStrategy implements SqlExecutionStrategy {

    @Override
    public void execute(Connection connection, ParseResultVo vo, ExecuteResult resultItem) throws Exception {
        int count = bigDataInsertOverSelect(connection, vo);
        resultItem.setStatus(ExecuteResult.STATUS_SUCCESS);
        resultItem.setData(count);
        resultItem.setType(ExecuteResult.TYPE_NON_SELECT);
    }

    private int bigDataInsertOverSelect(Connection connection, ParseResultVo vo) {
        try {
            List<Map<String, Object>> data = JdbcUtils.executeQuery(connection, vo.getSelectSql());
            if (data.isEmpty()) {
                log.warn("查询结果为空，无需插入");
                return 0;
            }
            String insertWithParamsSql = vo.getInsertWithParamsSql();
            String cleanInsertSql = insertWithParamsSql.replaceAll("(?i)INSERT INTO\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\.([a-zA-Z_][a-zA-Z0-9_]*)", "INSERT INTO $2");
            String schema = vo.getTargetTableInfo().getSchema();
            int insertCount = JdbcUtils.batchInsert(DataSourceFactory.getBigDataSourceKeyName(schema), cleanInsertSql, data);
            log.debug("批量插入完成，成功插入 {} 条记录", insertCount);
            return insertCount;
        } catch (Exception e) {
            log.error("SQL执行异常: {}", e.getMessage(), e);
            throw new RuntimeException("插入操作失败: " + e.getMessage(), e);
        }
    }
}
