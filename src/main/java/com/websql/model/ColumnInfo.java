package com.websql.model;

import lombok.Data;

/**
 * 列信息类
 */
@Data
public class ColumnInfo {
    private String columnName;
    private String dataType;
    private boolean nullable;

    public ColumnInfo(String columnName, String dataType) {
        this(columnName, dataType, true);
    }

    public ColumnInfo(String columnName, String dataType, boolean nullable) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.nullable = nullable;
    }
}