package com.websql.model;

import lombok.Data;

import java.util.List;

@Data
public class DataAnalysisQo {

    /**
     * 查询的结果,json格式
     */
    private Object sampleData;

    /**
     * 执行的sql
     */
    private String sql;

    /**
     * 数据源
     */
    private String dataBaseName;

    /**
     * 表名列表
     */
    private List<String> tableNameList;

    /**
     * 执行类型
     */
    private String executeType;

    /**
     * 追问内容，用于继续对话
     */
    private String question;

}
