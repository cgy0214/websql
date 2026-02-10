package com.websql.model;

/**
 * @ClassName SqlOperationType
 * @Description SQL操作类型枚举
 * @Author rabbit boy_0214@sina.com
 * @Date 2024/02/10 10:00
 */
public enum SqlOperationType {
    INSERT("INSERT", "插入"),
    SELECT("SELECT", "查询"),
    UPDATE("UPDATE", "更新"),
    DELETE("DELETE", "删除"),
    SELECT_INSERT("INSERT_SELECT", "查询并插入"),
    SELECT_UPSERT("UPSERT_SELECT", "查询并更新插入"),
    UNKNOWN("UNKNOWN", "未知");

    private final String code;
    private final String description;

    SqlOperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code;
    }

    /**
     * 根据SqlNode类型判断操作类型
     */
    public static SqlOperationType fromSqlNode(org.apache.calcite.sql.SqlNode sqlNode) {
        if (sqlNode == null) {
            return UNKNOWN;
        }
        
        String className = sqlNode.getClass().getSimpleName().toUpperCase();
        
        if (className.contains("INSERT")) {
            return INSERT;
        } else if (className.contains("SELECT")) {
            return SELECT;
        } else if (className.contains("UPDATE")) {
            return UPDATE;
        } else if (className.contains("DELETE")) {
            return DELETE;
        } else {
            return UNKNOWN;
        }
    }
}