package com.websql.model;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;

@Data
public class CalciteDataSourceParams {

    private String sourceKey;

    private DruidDataSource dataSource;

    private String catalog;

    private String schema;

    public CalciteDataSourceParams(String sourceKey, DruidDataSource dataSource) {
        this.sourceKey = sourceKey;
        this.dataSource = dataSource;
    }
    public CalciteDataSourceParams(String sourceKey, DruidDataSource dataSource, String catalog, String schema) {
        this.sourceKey = sourceKey;
        this.dataSource = dataSource;
        this.catalog = catalog;
        this.schema = schema;
    }
}
