package com.itboy.model;

import lombok.Data;

import java.util.Map;

/**
 * @ClassName : SqlExecuteResultVo
 * @Description : 返回对象
 * @Author :  超 boy_0214@sina.com
 * @Date: 2023/10/31 16:32
 */

@Data
public class SqlExecuteResultVo {

    /**
     * 呈现类型  0列表 1信息
     */
    private int type;

    /**
     * 执行类型
     */
    private String executeType;

    /**
     * 查询的表名
     */
    private String tableNameList;

    /**
     * 数据字段
     */
    private Map<String, Object> data;

    /**
     * 信息字段
     */
    private String message;

    /**
     * 执行SQL
     */
    private String parserSql;

    /**
     * 数据源KEY
     */
    private String dataBaseKey;

    /**
     * 耗时
     */
    private long time;
}
