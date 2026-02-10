package com.websql.service.strategy;

import com.websql.model.ExecuteResult;
import com.websql.model.ParseResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
public class ModificationStrategy implements SqlExecutionStrategy {

    @Override
    public void execute(Connection connection, ParseResultVo vo, ExecuteResult resultItem) throws Exception {
        int count = executeUpdate(connection, resultItem.getSql());
        resultItem.setStatus(ExecuteResult.STATUS_SUCCESS);
        resultItem.setData(count);
        resultItem.setType(ExecuteResult.TYPE_NON_SELECT);
    }

    private int executeUpdate(Connection connection, String sqlContent) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sqlContent);
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }
}
