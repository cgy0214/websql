package com.websql.service.strategy;

import com.websql.model.SqlOperationType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class SqlStrategyFactory {

    @Resource
    private SelectStrategy selectStrategy;
    @Resource
    private ModificationStrategy modificationStrategy;
    @Resource
    private InsertSelectStrategy insertSelectStrategy;
    @Resource
    private UpsertSelectStrategy upsertSelectStrategy;

    public SqlExecutionStrategy getStrategy(SqlOperationType type) {
        switch (type) {
            case SELECT: 
                return selectStrategy;
            case INSERT:
            case UPDATE:
            case DELETE: 
                return modificationStrategy;
            case SELECT_INSERT: 
                return insertSelectStrategy;
            case SELECT_UPSERT: 
                return upsertSelectStrategy;
            default: 
                throw new IllegalArgumentException("Unknown operation type: " + type);
        }
    }
}
