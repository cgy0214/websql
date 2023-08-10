package com.itboy.config;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.itboy.model.SysSetup;
import com.itboy.util.CacheUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @ClassName SqlDruidParser
 * @Description sql解析器
 * @Author 超 boy_0214@sina.com
 * @Date 2019/6/24 0024 10:16
 **/
@Slf4j
public class SqlDruidParser {


    /**
     * sql解析
     *
     * @param dbName 数据源
     * @param sql    sql文本
     * @return
     * @throws Exception
     */
    public static Map<String, Object> sqlParser(String dbName, String sql) throws Exception {
        Map<String, Object> resultMap = new HashMap<>(6);
        String dbType = DataSourceFactory.getDbType(dbName);
        String sqlFormat = SQLUtils.format(sql, dbType);
        String[] sqlParam = sqlFormat.split(";");
        SchemaStatVisitor visitor = checkRiskMethod(sql, dbType);
        Object method = getFirstOrNull(visitor.getTables());
        resultMap.put("tables", visitor.getTables());
        resultMap.put("fields", visitor.getColumns());
        resultMap.put("tableName", getTable(visitor.getTables()));
        resultMap.put("executeType", method == null ? "SELECT" : method.toString().toUpperCase());
        if (method != null && "SELECT".equalsIgnoreCase(method.toString())) {
            resultMap.put("executeSql", pageLimitSql(sqlParam, dbType));
        } else {
            resultMap.put("executeSql", Arrays.asList(sqlParam));
        }
        return resultMap;
    }

    private static Object getFirstOrNull(Map<TableStat.Name, TableStat> map) {
        Object obj = null;
        for (Map.Entry<TableStat.Name, TableStat> entry : map.entrySet()) {
            obj = entry.getValue();
            if (obj != null) {
                break;
            }
        }
        return obj;
    }

    private static Object getTable(Map<TableStat.Name, TableStat> map) {
        String tableName = null;
        for (Map.Entry<TableStat.Name, TableStat> entry : map.entrySet()) {
            TableStat.Name keys = entry.getKey();
            tableName = keys.getName();
            if (tableName != null) {
                break;
            }
        }
        return tableName;
    }

    /**
     * 分页判断限制最大条数,暂时写死处理
     *
     * @param sqlParam sql文本集合
     * @param dbType   数据源类型
     * @return
     */
    private static List<String> pageLimitSql(String[] sqlParam, String dbType) {
        List<String> executeSql = new ArrayList<>(sqlParam.length);
        Integer limitMax = 1000;
        SysSetup sysSetup = CacheUtils.get("sys_setup", SysSetup.class);
        if (ObjectUtil.isNotNull(sysSetup) && ObjectUtil.isNotNull(sysSetup.getPageLimitMax())) {
            limitMax = sysSetup.getPageLimitMax();
        }
        String page = "select * from (";
        String size = ") where rownum <= " + limitMax;
        //需要使用rowNum分页的数据库
        List<String> pageType = Arrays.asList("h2", "oracle");
        for (String item : sqlParam) {
            if (pageType.contains(dbType)) {
                if (!item.toUpperCase().contains("ROWNUM") && !item.toUpperCase().contains("ROW_NUMBER") && limitMax > 0) {
                    executeSql.add(page + item + size);
                } else {
                    executeSql.add(item);
                }
            } else {
                if (!item.toUpperCase().contains("LIMIT") && limitMax > 0) {
                    executeSql.add(item + "  LIMIT " + limitMax);
                } else {
                    executeSql.add(item);
                }
            }
        }
        return executeSql;
    }

    /**
     * 检查有分析的语句
     *
     * @param sql
     * @param dbType
     * @return
     */
    private static SchemaStatVisitor checkRiskMethod(String sql, String dbType) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, dbType);
        SchemaStatVisitor result = null;
        SysSetup sysSetup = CacheUtils.get("sys_setup", SysSetup.class);
        List<String> methods = new ArrayList<>();
        if (ObjectUtil.isNotNull(sysSetup) && ObjectUtil.isNotNull(sysSetup.getRiskText())) {
            methods.addAll(Arrays.asList(sysSetup.getRiskText().split(",")));
        }
        for (SQLStatement sqlStatement : stmtList) {
            SchemaStatVisitor visitor = SQLUtils.createSchemaStatVisitor(DbType.valueOf(dbType));
            sqlStatement.accept(visitor);
            if (ObjectUtil.isNull(result)) {
                result = visitor;
            }
            String method = Optional.ofNullable(getFirstOrNull(visitor.getTables())).orElse("").toString().toLowerCase();
            if (methods.contains(method)) {
                throw new RuntimeException("不允许执行【" + method + "】SQL语句,请联系管理员!");
            }
        }
        return result;
    }


}
