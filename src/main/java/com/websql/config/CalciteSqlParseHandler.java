package com.websql.config;

import cn.hutool.core.util.ObjectUtil;
import com.websql.model.*;
import lombok.Getter;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.dialect.AnsiSqlDialect;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParser.Config;
import org.apache.calcite.sql.util.SqlString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalciteSqlParseHandler {

    /**
     * 表信息提取结果封装类
     */
    @Getter
    private static class TableExtractionResult {
        private final List<String> tableNames;
        private final List<TableInfo> tableInfos;
        private final Set<String> schemas;

        public TableExtractionResult(List<String> tableNames, List<TableInfo> tableInfos, Set<String> schemas) {
            this.tableNames = tableNames;
            this.tableInfos = tableInfos;
            this.schemas = schemas;
        }

    }

    /**
     * 解包SqlNode，处理SqlOrderBy等包装节点
     * 当SQL包含LIMIT/OFFSET时，Calcite会将其包装在SqlOrderBy中
     * 
     * @param sqlNode 原始SqlNode
     * @return 解包后的实际SqlNode
     */
    private static SqlNode unwrapSqlNode(SqlNode sqlNode) {
        if (sqlNode instanceof org.apache.calcite.sql.SqlOrderBy) {
            org.apache.calcite.sql.SqlOrderBy orderBy = (org.apache.calcite.sql.SqlOrderBy) sqlNode;
            return orderBy.query;
        }
        return sqlNode;
    }

    /**
     * 解析SQL语句，支持INSERT/SELECT/UPDATE/DELETE等操作类型
     *
     * @param fullSql 完整的SQL语句
     * @return 包含解析结果的对象
     */
    public static ParseResultVo parseSql(String fullSql) {
        try {
            Config config = SqlParser.config()
                    .withCaseSensitive(false)
                    .withUnquotedCasing(Casing.UNCHANGED)
                    .withQuotedCasing(Casing.UNCHANGED);

            SqlParser parser = SqlParser.create(fullSql, config);
            SqlNode sqlNode = parser.parseStmt();

            SqlNode actualNode = unwrapSqlNode(sqlNode);
            SqlOperationType operationType = SqlOperationType.fromSqlNode(actualNode);

            ParseResultVo resultVo;
            switch (operationType) {
                case INSERT:
                    resultVo = handleInsertStatement((SqlInsert) actualNode);
                    break;
                case SELECT:
                    resultVo = handleSelectStatement((SqlSelect) actualNode);
                    break;
                case UPDATE:
                    resultVo = handleUpdateStatement((SqlUpdate) actualNode);
                    break;
                case DELETE:
                    resultVo = handleDeleteStatement((SqlDelete) actualNode);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的SQL语句类型: " + actualNode.getClass().getSimpleName());
            }

            if (sqlNode.toString().toUpperCase().contains("UPSERT") && SqlOperationType.SELECT_INSERT.equals(resultVo.getOperationType())) {
                resultVo.setOperationType(SqlOperationType.SELECT_UPSERT);
            }

            return resultVo;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 处理INSERT语句，提取目标表、列信息和源表信息
     */
    private static ParseResultVo handleInsertStatement(SqlInsert insertStmt) {
        SqlIdentifier targetTable = (SqlIdentifier) insertStmt.getTargetTable();
        SqlNodeList targetColumns = insertStmt.getTargetColumnList();
        SqlNode sourceNode = insertStmt.getSource();

        // 解析目标表信息
        TargetTableInfo targetTableInfo = extractTargetTableInfo(targetTable, targetColumns);

        // 解析源表信息
        TableExtractionResult sourceTableResult = new TableExtractionResult(new ArrayList<>(), new ArrayList<>(), new HashSet<>());
        if (sourceNode instanceof SqlSelect) {
            sourceTableResult = extractTableInfo((SqlSelect) sourceNode);
        }
        if (ObjectUtil.isNotNull(sourceTableResult)) {
            sourceTableResult.getSchemas().add(targetTableInfo.getSchema());
            sourceTableResult.getTableNames().add(targetTableInfo.getTableName());
        }
        // 生成SQL
        String selectSql = sourceNode instanceof SqlSelect ? buildSelectSql((SqlSelect) sourceNode) : null;
        String insertWithParams = buildInsertWithParams(targetTable, targetColumns);

        SqlOperationType operationType = selectSql==null? SqlOperationType.INSERT :  SqlOperationType.SELECT_INSERT;

        return  new ParseResultVo(selectSql, insertWithParams, targetTable.toString(),
                sourceTableResult.getTableNames(), sourceTableResult.getTableInfos(), sourceTableResult.getSchemas(),
                targetTableInfo, operationType);
    }

    /**
     * 处理SELECT语句，提取源表信息
     */
    private static ParseResultVo handleSelectStatement(SqlSelect selectStmt) {
        TableExtractionResult extractionResult = extractTableInfo(selectStmt);
        String selectSql = buildSelectSql(selectStmt);

        return new ParseResultVo(selectSql,  null, null,
                extractionResult.getTableNames(), extractionResult.getTableInfos(), extractionResult.getSchemas(),
                null, SqlOperationType.SELECT);
    }

    /**
     * 处理UPDATE语句
     */
    private static ParseResultVo handleUpdateStatement(SqlUpdate updateStmt) {
        SqlIdentifier targetTable = (SqlIdentifier) updateStmt.getTargetTable();

        List<String> tableNames = new ArrayList<>();
        List<TableInfo> tableInfos = new ArrayList<>();
        Set<String> schemas = new HashSet<>();

        extractTableInfoRecursive(targetTable, tableNames, tableInfos, schemas);

        TableExtractionResult extractionResult = new TableExtractionResult(tableNames, tableInfos, schemas);

        return new ParseResultVo(null,  null, targetTable.toString(),
                extractionResult.getTableNames(), extractionResult.getTableInfos(), extractionResult.getSchemas(),
                null, SqlOperationType.UPDATE);
    }

    /**
     * 处理DELETE语句
     */
    private static ParseResultVo handleDeleteStatement(SqlDelete deleteStmt) {
        SqlIdentifier targetTable = (SqlIdentifier) deleteStmt.getTargetTable();

        // 复用现有的表信息提取方法
        List<String> tableNames = new ArrayList<>();
        List<TableInfo> tableInfos = new ArrayList<>();
        Set<String> schemas = new HashSet<>();

        extractTableInfoRecursive(targetTable, tableNames, tableInfos, schemas);

        TableExtractionResult extractionResult = new TableExtractionResult(tableNames, tableInfos, schemas);

        return new ParseResultVo(null,  null, targetTable.toString(),
                extractionResult.getTableNames(), extractionResult.getTableInfos(), extractionResult.getSchemas(),
                null, SqlOperationType.DELETE);
    }

    /**
     * 提取目标表详细信息
     */
    private static TargetTableInfo extractTargetTableInfo(SqlIdentifier targetTable, SqlNodeList targetColumns) {
        List<String> names = targetTable.names;
        String schema = names.size() > 1 ? names.get(0) : null;
        String tableName = names.size() > 1 ? names.get(1) : names.get(0);

        List<ColumnInfo> columns = new ArrayList<>();
        if (targetColumns != null) {
            for (SqlNode column : targetColumns) {
                if (column instanceof SqlIdentifier) {
                    columns.add(new ColumnInfo(((SqlIdentifier) column).getSimple(), "UNKNOWN"));
                }
            }
        }

        return new TargetTableInfo(schema, tableName, columns);
    }

    /**
     * 将SqlSelect转换为标准SQL字符串
     */
    private static String buildSelectSql(SqlSelect select) {
        SqlString sqlString = select.toSqlString(AnsiSqlDialect.DEFAULT);
        return formatSql(sqlString.getSql());
    }

    /**
     * 构建带参数占位符的INSERT语句（用于JDBC批量插入）
     */
    private static String buildInsertWithParams(SqlIdentifier table, SqlNodeList columns) {
        StringBuilder sb = new StringBuilder();
        String tableStr = table.toString().replaceAll("[\"`]", "");
        sb.append("INSERT INTO ").append(tableStr);
        if (columns != null && !columns.isEmpty()) {
            String columnsStr = columns.toString().replaceAll("[\"`]", "");
            sb.append("\n    (").append(columnsStr).append(")");
        }
        if (columns != null) {
            String placeholders = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
            sb.append("\nVALUES (").append(placeholders).append(")");
        }
        return sb.toString();
    }

    /**
     * 提取SELECT语句中所有的表信息
     *
     * @param select SqlSelect对象
     * @return 表信息结果对象
     */
    private static TableExtractionResult extractTableInfo(SqlSelect select) {
        List<String> tableNames = new ArrayList<>();
        List<TableInfo> tableInfos = new ArrayList<>();
        Set<String> schemas = new HashSet<>();

        SqlNode fromNode = select.getFrom();

        if (fromNode != null) {
            extractTableInfoRecursive(fromNode, tableNames, tableInfos, schemas);
        }

        return new TableExtractionResult(tableNames, tableInfos, schemas);
    }

    /**
     * 递归提取表信息（包含schema和table）
     *
     * @param node       当前节点
     * @param tableNames 表名列表（兼容旧格式）
     * @param tableInfos 表信息列表
     * @param schemas    schema集合（自动去重）
     */
    private static void extractTableInfoRecursive(SqlNode node, List<String> tableNames,
                                                  List<TableInfo> tableInfos, Set<String> schemas) {
        if (node instanceof SqlIdentifier) {
            // 单个表名
            SqlIdentifier identifier = (SqlIdentifier) node;
            // 获取完整的名称列表
            List<String> names = identifier.names;

            if (names.size() == 1) {
                // 只有表名，没有schema，认为是别名，直接跳过
                return;
            } else if (names.size() >= 2) {
                // 正常的 schema.table 格式
                String schema = names.get(0);
                String tableName = names.get(1);

                TableInfo tableInfo = new TableInfo(schema, tableName);
                if (!tableInfos.contains(tableInfo)) {
                    tableInfos.add(tableInfo);
                    tableNames.add(schema + "." + tableName); // 兼容旧格式
                    schemas.add(schema); // 自动去重
                }
            }
        } else if (node instanceof SqlJoin) {
            // 处理JOIN结构
            SqlJoin join = (SqlJoin) node;
            extractTableInfoRecursive(join.getLeft(), tableNames, tableInfos, schemas);
            extractTableInfoRecursive(join.getRight(), tableNames, tableInfos, schemas);
        } else if (node instanceof SqlBasicCall) {
            // 处理函数调用等情况
            SqlBasicCall call = (SqlBasicCall) node;
            for (SqlNode operand : call.getOperandList()) {
                if (operand != null) {
                    extractTableInfoRecursive(operand, tableNames, tableInfos, schemas);
                }
            }
        }
    }

    /**
     * 简单的SQL格式化（美化输出）
     */
    private static String formatSql(String sql) {
        return sql.replaceAll("FROM", "\nFROM")
                .replaceAll("WHERE", "\nWHERE")
                .replaceAll("JOIN", "\n  JOIN")
                .replaceAll("ON", "\n    ON")
                .replaceAll("SELECT", "SELECT\n    ")
                .replaceAll(",", ",\n    ");
    }

}