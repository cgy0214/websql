package com.websql.model;

import lombok.Data;

/**
 * SQL 执行结果模型
 */
@Data
public class ExecuteResult {
    /**
     * 原始 SQL
     */
    private String sql;

    /**
     * 执行状态
     * 1: 成功
     * 2: 失败
     */
    private Integer status;

    /**
     * SQL 类型
     * 0: SELECT 查询
     * 1: UPDATE/INSERT/DELETE 非查询
     */
    private Integer type;

    /**
     * 执行结果数据
     * 查询：List<Map<String, Object>>
     * 非查询：Integer (影响行数)
     */
    private Object data;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时 (ms)
     */
    private Long time;

    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAIL = 2;
    public static final int TYPE_SELECT = 0;
    public static final int TYPE_NON_SELECT = 1;
}
