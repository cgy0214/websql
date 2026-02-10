package com.websql.service.strategy;

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
public class SelectStrategy implements SqlExecutionStrategy {

    @Override
    public void execute(Connection connection, ParseResultVo vo, ExecuteResult resultItem) throws Exception {
        List<Map<String, Object>> data = JdbcUtils.executeQuery(connection, resultItem.getSql());
        resultItem.setStatus(ExecuteResult.STATUS_SUCCESS);
        resultItem.setData(data);
        resultItem.setType(ExecuteResult.TYPE_SELECT);
    }
}
