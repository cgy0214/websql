package com.itboy.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DataSourceFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName TableFieldSqlUtils
 * @Description sql组装
 * @Author 超 boy_0214@sina.com
 * @Date 2023/08/08 14:15
 **/
public class TableFieldSqlUtils {

    private static final Map<String, String> DATA_BASE_TABLE_VIEW_SQL = MapUtil.builder(new HashMap<String, String>(5))
            .put("mysql", " SELECT TABLE_NAME AS TABLE_NAME,COLUMN_NAME TABLE_FIELD from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA= (select database())  ")

            .put("oracle", " SELECT table_name AS TABLE_NAME, column_name AS TABLE_FIELD from user_col_comments ")

            .put("h2", " SELECT TABLE_NAME AS TABLE_NAME,COLUMN_NAME TABLE_FIELD from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' ")

            .put("dm", " SELECT TABLE_NAME AS TABLE_NAME,COLUMN_NAME AS TABLE_FIELD from all_tab_columns where owner not in('SYS','SYSDBA','CTISYS') ")

            .put("kingbase", " SELECT table_name AS TABLE_NAME,column_name AS TABLE_FIELD from  INFORMATION_SCHEMA.COLUMNS where table_schema='public' ")

            .put("postgresql", " SELECT table_name AS \"TABLE_NAME\",column_name AS \"TABLE_FIELD\"  from  INFORMATION_SCHEMA.COLUMNS where table_schema='public' ")

            .put("oscar", " SELECT TABLE_NAME AS TABLE_NAME,COLUMN_NAME AS TABLE_FIELD from all_tab_columns where owner not in('SYS','SYSDBA','CTISYS') ")

            .build();


    public static String getViewSql(String database) {
        String dbType = DataSourceFactory.getDbType(database);
        if (ObjectUtil.isEmpty(dbType)) {
            return null;
        }
        return DATA_BASE_TABLE_VIEW_SQL.get(dbType);
    }
}
