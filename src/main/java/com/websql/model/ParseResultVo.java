package com.websql.model;

import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Data
@Getter
public class ParseResultVo {

    // SQL内容相关
    private String selectSql;              // SELECT查询语句
    private String insertWithParamsSql;    // 带参数的INSERT

    // 表信息相关
    private String targetTable;            // 目标表名（兼容旧版本）
    private TargetTableInfo targetTableInfo; // 目标表详细信息
    private List<String> tableNames;       // 所有涉及的表名（保持兼容性）
    private List<TableInfo> tableInfos;    // 表信息列表（包含schema和table）
    private Set<String> schemas;           // 去重后的schema列表

    private SqlOperationType operationType;  // SQL操作类型

    public ParseResultVo(String selectSql,
                         String insertWithParamsSql, String targetTable,
                         List<String> tableNames, List<TableInfo> tableInfos,
                         Set<String> schemas, TargetTableInfo targetTableInfo,
                         SqlOperationType operationType) {
        this.selectSql = selectSql;
        this.insertWithParamsSql = insertWithParamsSql;
        this.targetTable = targetTable;
        this.tableNames = tableNames;
        this.tableInfos = tableInfos;
        this.schemas = schemas;
        this.targetTableInfo = targetTableInfo;
        this.operationType = operationType;
    }

}