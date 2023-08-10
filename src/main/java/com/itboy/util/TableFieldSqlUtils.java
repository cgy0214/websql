package com.itboy.util;

import com.itboy.config.DataSourceFactory;

/**
 * @ClassName TableFieldSqlUtils
 * @Description sql组装
 * @Author 超 boy_0214@sina.com
 * @Date 2023/08/08 14:15
 **/
public class TableFieldSqlUtils {


    private static final String FIND_TABLE_MYSQL = " SELECT TABLE_NAME AS NAME,COLUMN_NAME FIELD from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA= (select database())  ";

    private static final String FIND_TABLE_ORACLE = " select table_name AS NAME, column_name AS FIELD from user_col_comments ";

    private static final String FIND_TABLE_H2 = " SELECT TABLE_NAME AS NAME,COLUMN_NAME FIELD from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' ";


    private static final String FIND_TABLE_DM = "select TABLE_NAME AS NAME,COLUMN_NAME AS FIELD from all_tab_columns where owner not in('SYS','SYSDBA','CTISYS') ";

    public static String getViewSql(String database) {
        switch (DataSourceFactory.getDbType(database)) {
            case "mysql":
                return FIND_TABLE_MYSQL;
            case "oracle":
                return FIND_TABLE_ORACLE;
            case "h2":
                return FIND_TABLE_H2;
            case "dm":
                return FIND_TABLE_DM;
            default:
                return null;
        }
    }
}
