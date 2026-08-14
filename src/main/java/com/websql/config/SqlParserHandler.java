package com.websql.config;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.PagerUtils;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.*;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.websql.model.SqlOperationType;
import com.websql.model.SqlParserVo;
import com.websql.model.SysSetup;
import com.websql.util.CacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @ClassName : SqlParserHandler
 * @Description : sql语法解析
 * @Author : rabbit boy_0214@sina.com
 * @Date: 2023/10/31 11:38
 */
public class SqlParserHandler {

    private static final Logger logger = LoggerFactory.getLogger(SqlParserHandler.class);

    private static final Pattern AGGREGATE_PATTERN = Pattern.compile("\\b(COUNT|SUM|AVG|MIN|MAX|GROUP)\\b", Pattern.CASE_INSENSITIVE);

    // 国产数据库druid分页工具类不支持，按照类似的数据库类型生成转换
    private static final Map<String, DbType> PAGE_DB_TYPE = new HashMap<>();

    static {
        PAGE_DB_TYPE.put(DbType.dm.name(), DbType.mysql);
        PAGE_DB_TYPE.put(DbType.kingbase.name(), DbType.mysql);
        PAGE_DB_TYPE.put(DbType.oscar.name(), DbType.mysql);
    }


    public static List<SqlParserVo> getParserVo(String databaseKey, String sql) {
        return getParserVo(databaseKey, sql, false);
    }

    public static List<SqlParserVo> getParserVo(String databaseKey, String sql, boolean skipPageLimit) {
        String type = DataSourceFactory.getDbType(databaseKey);
        if (ObjectUtil.isEmpty(type)) {
            throw new RuntimeException(databaseKey + "未获取到数据库类型，请检查数据源连接或重试一次！");
        }
        DbType dbType = DbType.of(type);
        List<SqlParserVo> resultList = new ArrayList<>();
        //todo 需兼容doris
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, dbType);
        for (SQLStatement statement : statements) {
            SchemaStatVisitor visitor = SQLUtils.createSchemaStatVisitor(dbType);
            statement.accept(visitor);

            String methodType = getExecuteType(visitor.getTables(), statement);
            List<String> riskMethod = checkRiskMethod(methodType);
            if (ObjectUtil.isNotNull(riskMethod) && !riskMethod.isEmpty()) {
                throw new RuntimeException("不允许执行【" + String.join(",", riskMethod) + "】SQL语句,请联系管理员!");
            }
            List<String> tableNameList = getTableName(visitor.getTables());
            List<String> tableColumns = getTableColumns(visitor.getColumns());

            SqlParserVo sqlParserVo = new SqlParserVo();
            sqlParserVo.setMethodType(methodType);
            sqlParserVo.setTableNameList(tableNameList);
            sqlParserVo.setTableColumns(tableColumns);
            if ("SELECT".equals(methodType)) {
                sqlParserVo.setSqlContent(pageLimitSql(statement, skipPageLimit));
            } else {
                String formatSql = SQLUtils.toSQLString(statement, statement.getDbType());
                sqlParserVo.setSqlContent(formatSql);
            }
            resultList.add(sqlParserVo);
        }
        return resultList;
    }

    /**
     * 获取操作类型
     *
     * @param tables    解析出来的表名
     * @param statement 节点
     * @return
     */
    private static String getExecuteType(Map tables, SQLStatement statement) {
        // 优先根据 statement 类型精确判断（如 CREATE VIEW / CREATE TABLE AS SELECT 等包含子查询的场景）
        if (statement instanceof SQLInsertStatement) {
            return SqlOperationType.INSERT.getCode();
        } else if (statement instanceof SQLUpdateStatement) {
            return SqlOperationType.UPDATE.getCode();
        } else if (statement instanceof SQLDeleteStatement) {
            return SqlOperationType.DELETE.getCode();
        } else if (statement instanceof SQLCreateStatement) {
            return SqlOperationType.UPDATE.getCode();
        } else if (statement instanceof SQLTruncateStatement) {
            return "TRUNCATE";
        } else if (statement instanceof SQLCallStatement) {
            return "CALL";
        } else if (statement instanceof MySqlLoadDataInFileStatement || statement instanceof MySqlLoadXmlStatement) {
            return "LOAD";
        } else if (statement instanceof MySqlRenameTableStatement) {
            return "RENAME";
        } else if (statement instanceof SQLLockTableStatement || statement instanceof MySqlUnlockTablesStatement) {
            return "LOCK";
        } else if (statement instanceof SQLGrantStatement) {
            return "GRANT";
        } else if (statement instanceof SQLRevokeStatement) {
            return "REVOKE";
        } else if (statement instanceof MySqlAlterUserStatement) {
            return "ALTERUSER";
        } else if (statement instanceof MySqlFlushStatement) {
            return "FLUSH";
        } else if (statement instanceof SQLAnalyzeTableStatement) {
            return "ANALYZE";
        } else if (statement instanceof SQLOptimizeStatement || statement instanceof MySqlOptimizeStatement) {
            return "OPTIMIZE";
        } else if (statement instanceof MySqlCheckTableStatement) {
            return "CHECK";
        } else if (statement instanceof SQLCommentStatement) {
            return "COMMENT";
        }
        // statement 类型无法判断时，回退到 visitor 解析的表操作类型
        if (ObjectUtil.isNotNull(tables) && !tables.isEmpty()) {
            String selectType = null;
            for (Object value : tables.values()) {
                if (ObjectUtil.isNotNull(value) && !value.toString().isEmpty()) {
                    String operationType = value.toString().toUpperCase();
                    if (!SqlOperationType.SELECT.getCode().equals(operationType)) {
                        return operationType;
                    }
                    if (ObjectUtil.isNull(selectType)) {
                        selectType = operationType;
                    }
                }
            }
            if (ObjectUtil.isNotNull(selectType)) {
                return selectType;
            }
        }
        return SqlOperationType.SELECT.getCode();
    }


    /**
     * 默认查询分页限制
     *
     * @param sqlStatement   sql语句
     * @param skipPageLimit  是否跳过默认分页限制（如导出场景）
     * @return 分页后的sql语句
     */
    private static String pageLimitSql(SQLStatement sqlStatement, boolean skipPageLimit) {
        String sql = SQLUtils.toSQLString(sqlStatement, sqlStatement.getDbType());
        if (!skipPageLimit && sqlStatement instanceof SQLSelectStatement) {
            SQLSelectStatement sqlSelectStatement = (SQLSelectStatement) sqlStatement;
            if (sqlSelectStatement.getSelect().getQuery() instanceof SQLSelectQueryBlock) {
                SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) sqlSelectStatement.getSelect().getQuery();
                for (SQLSelectItem sqlSelectItem : sqlSelectQueryBlock.getSelectList()) {
                    String method = sqlSelectItem.getExpr().toString();
                    if (AGGREGATE_PATTERN.matcher(method).find()) {
                        return sql;
                    }
                }
                if (ObjectUtil.isNotNull(sqlSelectQueryBlock.getGroupBy())) {
                    return sql;
                }
                if (ObjectUtil.isNull(sqlSelectQueryBlock.getLimit()) && !sql.toUpperCase().contains("ROWNUM")) {
                    try {
                        Integer limitMax = 1000;
                        SysSetup sysSetup = CacheUtils.get("sys_setup", SysSetup.class);
                        if (ObjectUtil.isNotNull(sysSetup) && ObjectUtil.isNotNull(sysSetup.getPageLimitMax())) {
                            limitMax = sysSetup.getPageLimitMax();
                        }
                        return PagerUtils.limit(sql, getPageType(sqlSelectStatement.getDbType()), 0, limitMax);
                    } catch (Exception e) {
                        logger.error("生成分页拦截SQL失败，不支持{}数据库类型,可以关闭分页拦截继续使用。", sqlSelectStatement.getDbType(), e);
                    }
                }
            }
        }
        return sql;
    }


    /**
     * 国产数据库druid工具类不支持，只能按照类似的数据库类型生成转换
     *
     * @param dbType 数据库类型
     * @return 转换后的数据库类型
     */
    private static DbType getPageType(DbType dbType) {
        return PAGE_DB_TYPE.getOrDefault(dbType.name(), dbType);
    }

    /**
     * 黑名单语法过滤
     *
     * @param executeType 操作类型
     * @return 过滤后的操作类型
     */
    private static List<String> checkRiskMethod(String executeType) {
        SysSetup sysSetup = CacheUtils.get("sys_setup", SysSetup.class);
        if (ObjectUtil.isNotNull(sysSetup) && ObjectUtil.isNotNull(sysSetup.getRiskText())) {
            List<String> riskList = new ArrayList<>(Arrays.asList(sysSetup.getRiskText().split(",")));
            return riskList.stream().filter(s -> s.toUpperCase().equals(executeType)).collect(Collectors.toList());
        }
        return null;
    }


    /**
     * 获取所有表名
     *
     * @return 表名列表
     */
    private static List<String> getTableName(Map tables) {
        List<String> tableList = new ArrayList<>(tables.size());
        if (ObjectUtil.isNotNull(tables) && !tables.isEmpty()) {
            for (Object value : tables.keySet()) {
                if (ObjectUtil.isNotNull(value) && !value.toString().isEmpty()) {
                    tableList.add(value.toString());
                }
            }
        }
        return tableList;
    }


    /**
     * 获取所有列名
     *
     * @return 列名列表
     */
    private static List<String> getTableColumns(Collection<TableStat.Column> columns) {
        List<String> columnList = new ArrayList<>(columns.size());
        for (TableStat.Column column : columns) {
            columnList.add(column.getName());
        }
        return columnList;
    }

}
