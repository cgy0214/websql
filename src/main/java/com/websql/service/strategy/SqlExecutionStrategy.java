package com.websql.service.strategy;

import com.websql.model.ExecuteResult;
import com.websql.model.ParseResultVo;

import java.sql.Connection;

public interface SqlExecutionStrategy {
    void execute(Connection connection, ParseResultVo vo, ExecuteResult resultItem) throws Exception;
}
