package com.websql.model;

import lombok.Data;

/**
 * 表信息类，包含schema和表名
 */
@Data
public class TableInfo {
    private String schema;
    private String tableName;

    public TableInfo(String schema, String tableName) {
        this.schema = schema;
        this.tableName = tableName;
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