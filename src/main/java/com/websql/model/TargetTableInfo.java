package com.websql.model;

import lombok.Data;

import java.util.List;

/**
 * 目标表信息类
 */
@Data
public class TargetTableInfo {
    private String schema;
    private String tableName;
    private List<ColumnInfo> columns;

    public TargetTableInfo(String schema, String tableName, List<ColumnInfo> columns) {
        this.schema = schema;
        this.tableName = tableName;
        this.columns = columns;
    }

    /**
     * 获取完整表名 (schema.table)
     */
    public String getFullName() {
        if (schema != null && !schema.isEmpty()) {
            return schema + "." + tableName;
        }
        return tableName;
    }
}