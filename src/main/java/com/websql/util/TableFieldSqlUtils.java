package com.websql.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.websql.config.DataSourceFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName TableFieldSqlUtils
 * @Description sql组装
 * @Author rabbit boy_0214@sina.com
 * @Date 2023/08/08 14:15
 **/
public class TableFieldSqlUtils {


    /**
     * 不同数据库的建表语句查询SQL
     */
    private static final Map<String, String> DATA_BASE_SHOW_CREATE_TABLE_SQL = MapUtil.builder(new HashMap<String, String>(8))
            .put("mysql", "SHOW CREATE TABLE {}")
            .put("oracle", "SELECT DBMS_METADATA.GET_DDL('TABLE', '{}') AS create_table_sql FROM DUAL")
            .put("postgresql", "SELECT 'CREATE TABLE ' || relname || ' (' || STRING_AGG(attname || ' ' || pg_catalog.format_type(atttypid, atttypmod) || CASE WHEN attnotnull THEN ' NOT NULL' ELSE '' END, ', ' ORDER BY attnum) || ');' AS create_table_sql FROM (SELECT c.relname, a.attname, a.atttypid, a.atttypmod, a.attnotnull, a.attnum FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid JOIN pg_attribute a ON a.attrelid = c.oid WHERE c.relkind = 'r' AND a.attnum > 0 AND n.nspname NOT IN ('information_schema', 'pg_catalog') AND c.relname = '{}') sub GROUP BY relname")
            .put("dm", "SELECT DBMS_METADATA.GET_DDL('TABLE', '{}') AS create_table_sql FROM DUAL")
            .put("kingbase", "SELECT DBMS_METADATA.GET_DDL('TABLE', '{}') AS create_table_sql FROM DUAL")
            .put("oscar", "SELECT DBMS_METADATA.GET_DDL('TABLE', '{}') AS create_table_sql FROM DUAL")
            .put("clickhouse", "SELECT create_table_query AS create_table_sql FROM system.tables WHERE database = (SELECT database()) AND name = '{}'")
            .put("h2", "SELECT 'CREATE TABLE ' || TABLE_NAME || ' (' || GROUP_CONCAT(COLUMN_NAME || ' ' || DATA_TYPE || CASE WHEN CHARACTER_OCTET_LENGTH IS NOT NULL THEN '(' || CHARACTER_OCTET_LENGTH || ')' ELSE '' END || CASE WHEN IS_NULLABLE = 'NO' THEN ' NOT NULL' ELSE '' END) || ');' AS create_table_sql FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '{}' GROUP BY TABLE_NAME")
            .build();

    
    /**
     * 查询列名
     */
    private static final Map<String, String> DATA_BASE_TABLE_VIEW_SQL = MapUtil.builder(new HashMap<String, String>(8))
            .put("mysql", " SELECT TABLE_NAME,COLUMN_NAME AS TABLE_FIELD FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=(SELECT DATABASE()) AND TABLE_NAME IN (SELECT TABLE_NAME FROM (SELECT DISTINCT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=(SELECT DATABASE()) ORDER BY TABLE_NAME LIMIT 1000) T) ORDER BY TABLE_NAME,ORDINAL_POSITION ")

            .put("oracle", "  SELECT * FROM (SELECT DISTINCT table_name AS TABLE_NAME, column_name AS TABLE_FIELD FROM user_col_comments ORDER BY table_name )  WHERE ROWNUM <= 1000 ")

            .put("h2", " SELECT TABLE_NAME AS TABLE_NAME,COLUMN_NAME TABLE_FIELD from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' ")

            .put("dm", " SELECT TABLE_NAME AS TABLE_NAME,COLUMN_NAME AS TABLE_FIELD from all_tab_columns where owner not in('SYS','SYSDBA','CTISYS') AND TABLE_NAME IN (SELECT TABLE_NAME FROM (SELECT DISTINCT TABLE_NAME FROM all_tab_columns WHERE owner not in('SYS','SYSDBA','CTISYS') ORDER BY TABLE_NAME FETCH FIRST 1000 ROWS ONLY) T) ORDER BY TABLE_NAME,COLUMN_ID ")

            .put("kingbase", " SELECT table_name AS TABLE_NAME,column_name AS TABLE_FIELD from  INFORMATION_SCHEMA.COLUMNS where table_schema=(SELECT current_schema()) AND TABLE_NAME IN (SELECT TABLE_NAME FROM (SELECT DISTINCT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema=(SELECT current_schema()) ORDER BY TABLE_NAME LIMIT 1000) T) ORDER BY TABLE_NAME,ORDINAL_POSITION ")

            .put("postgresql", "SELECT c.relname AS \"TABLE_NAME\", a.attname AS \"TABLE_FIELD\" FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid JOIN pg_attribute a ON a.attrelid = c.oid WHERE c.relkind = 'r' AND a.attnum > 0 AND n.nspname NOT IN ('information_schema', 'pg_catalog') AND c.relname IN (SELECT relname FROM (SELECT DISTINCT c2.relname FROM pg_class c2 JOIN pg_namespace n2 ON c2.relnamespace = n2.oid WHERE c2.relkind = 'r' AND n2.nspname NOT IN ('information_schema', 'pg_catalog') ORDER BY c2.relname LIMIT 1000) t) ORDER BY c.relname, a.attnum ")

            .put("oscar", " SELECT TABLE_NAME AS TABLE_NAME,COLUMN_NAME AS TABLE_FIELD from all_tab_columns where owner not in('SYS','SYSDBA','CTISYS') AND TABLE_NAME IN (SELECT TABLE_NAME FROM (SELECT DISTINCT TABLE_NAME FROM all_tab_columns WHERE owner not in('SYS','SYSDBA','CTISYS') ORDER BY TABLE_NAME LIMIT 1000) T) ORDER BY TABLE_NAME,COLUMN_ID ")

            .put("clickhouse"," SELECT t.name AS TABLE_NAME,c.name AS TABLE_FIELD FROM system.tables t JOIN system.columns c ON t.database=c.database AND t.name=c.table WHERE t.database=(SELECT database()) AND t.name IN (SELECT name FROM (SELECT name FROM system.tables WHERE database=(SELECT database()) ORDER BY name LIMIT 1000) AS x) ORDER BY t.name,c.position ")
            .build();

    /**
     * 生成默认的insert语句
     */
    private static final Map<String, String> DATA_BASE_TABLE_INSERT_SQL = MapUtil.builder(new HashMap<String, String>(5))
            .put("mysql", " SELECT CONCAT( 'insert into ', table_name, '(', GROUP_CONCAT( COLUMN_NAME ),') value (', GROUP_CONCAT( '#{',COLUMN_NAME,'}' ), ')' ) as create_table_sql FROM information_schema.COLUMNS WHERE table_schema = '{}' AND table_name = '{}'  ")
            .put("oracle", " SELECT 'INSERT INTO ' || table_name || '(' || LISTAGG(column_name, ',') WITHIN GROUP (ORDER BY column_id) || ') VALUES (' || LISTAGG(':\"' || column_name || '\"', ',') WITHIN GROUP (ORDER BY column_id) || ')' AS create_table_sql FROM all_tab_columns WHERE owner = UPPER('{}') AND table_name = UPPER('{}') ")
            .put("h2", " SELECT 'INSERT INTO ' || TABLE_NAME || '(' || GROUP_CONCAT(COLUMN_NAME) || ') VALUES (' || GROUP_CONCAT(':\"' || COLUMN_NAME || '\"') || ')' AS create_table_sql FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = UPPER('{}') AND TABLE_NAME = UPPER('{}') ")
            .put("dm", " SELECT 'INSERT INTO ' || TABLE_NAME || '(' || LISTAGG(COLUMN_NAME, ',') WITHIN GROUP (ORDER BY COLUMN_ID) || ') VALUES (' || LISTAGG(':\"' || COLUMN_NAME || '\"', ',') WITHIN GROUP (ORDER BY COLUMN_ID) || ')' AS create_table_sql FROM ALL_TAB_COLUMNS WHERE OWNER = UPPER('{}') AND TABLE_NAME = UPPER('{}') ")
            .put("kingbase", " SELECT 'INSERT INTO ' || table_name || '(' || string_agg(column_name, ',' ORDER BY ordinal_position) || ') VALUES (' || string_agg(':\"' || column_name || '\"', ',') || ')' AS create_table_sql FROM information_schema.columns WHERE table_schema = '{}' AND table_name = '{}' ")
            .put("postgresql", " SELECT 'INSERT INTO ' || c.table_name || '(' || string_agg(c.column_name, ',' ORDER BY c.ordinal_position) || ') VALUES (' || string_agg(':\"' || c.column_name || '\"', ',') || ')' AS create_table_sql FROM information_schema.columns c JOIN information_schema.tables t ON c.table_name = t.table_name AND c.table_schema = t.table_schema  and t.table_catalog = '{}'  WHERE c.table_name = '{}' GROUP BY c.table_name ")
            .put("oscar", " SELECT 'INSERT INTO ' || table_name || '(' || LISTAGG(column_name, ',') WITHIN GROUP (ORDER BY column_id) || ') VALUES (' || LISTAGG(':\"' || column_name || '\"', ',') WITHIN GROUP (ORDER BY column_id) || ')' AS create_table_sql FROM all_tab_columns WHERE owner = UPPER('{}') AND table_name = UPPER('{}') ")
            .put("clickhouse", " SELECT 'INSERT INTO ' || table || '(' || groupArray(name) || ') VALUES (' || groupArray(':\"' || name || '\"') || ')' AS create_table_sql FROM system.columns WHERE database = '{}' AND table = '{}' ")
            .build();


    public static String getViewSql(String database) {
        String dbType = DataSourceFactory.getDbType(database);
        if (ObjectUtil.isEmpty(dbType)) {
            return null;
        }
        return DATA_BASE_TABLE_VIEW_SQL.get(dbType);
    }

    public static String getInsertSql(String database,String tableName) {
        String dbType = DataSourceFactory.getDbType(database);
        if (ObjectUtil.isEmpty(dbType)) {
            return null;
        }
        String sqlTemplate = DATA_BASE_TABLE_INSERT_SQL.get(dbType);
        if (ObjectUtil.isEmpty(sqlTemplate)) {
            return null;
        }
        String dataBaseName = DataSourceFactory.getDataBaseName(database);
        return StrUtil.format(sqlTemplate, dataBaseName,tableName);
    }
    
    /**
     * 获取不同数据库的建表语句SQL
     * @param database 数据库类型
     * @param tableName 表名
     * @return 建表语句SQL
     */
    public static String getShowCreateTableSql(String database, String tableName) {
        String dbType = DataSourceFactory.getDbType(database);
        if (ObjectUtil.isEmpty(dbType)) {
            return null;
        }
        String sqlTemplate = DATA_BASE_SHOW_CREATE_TABLE_SQL.get(dbType);
        if (ObjectUtil.isEmpty(sqlTemplate)) {
            return null;
        }
        return sqlTemplate.replace("{}", tableName);
    }
}
