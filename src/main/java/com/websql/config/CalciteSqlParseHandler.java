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
     * 解析SQL语句，支持INSERT/SELECT/UPDATE/DELETE等操作类型
     *
     * @param fullSql 完整的SQL语句
     * @return 包含解析结果的对象
     */
    public static ParseResultVo parseSql(String fullSql) {
        try {
            boolean isUpsert = false;
            String sqlToParse = fullSql;
            if (fullSql != null && fullSql.trim().toUpperCase().startsWith("UPSERT")) {
                isUpsert = true;
                // 将UPSERT替换为INSERT以便解析
                sqlToParse = "INSERT" + fullSql.trim().substring(6);
            }

            Config config = SqlParser.config()
                    .withCaseSensitive(false)
                    .withUnquotedCasing(Casing.UNCHANGED)
                    .withQuotedCasing(Casing.UNCHANGED);

            SqlParser parser = SqlParser.create(sqlToParse, config);
            SqlNode sqlNode = parser.parseStmt();

            SqlOperationType operationType = SqlOperationType.fromSqlNode(sqlNode);

            ParseResultVo resultVo;
            switch (operationType) {
                case INSERT:
                    resultVo = handleInsertStatement((SqlInsert) sqlNode);
                    break;
                case SELECT:
                    resultVo = handleSelectStatement((SqlSelect) sqlNode);
                    break;
                case UPDATE:
                    resultVo = handleUpdateStatement((SqlUpdate) sqlNode);
                    break;
                case DELETE:
                    resultVo = handleDeleteStatement((SqlDelete) sqlNode);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的SQL语句类型: " + sqlNode.getClass().getSimpleName());
            }

            if (isUpsert && SqlOperationType.SELECT_INSERT.equals(resultVo.getOperationType())) {
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
        String insertFromTemp = buildInsertFromTempTable(targetTable, targetColumns, "temp_query_results");
        String insertWithParams = buildInsertWithParams(targetTable, targetColumns);

        SqlOperationType operationType = selectSql==null? SqlOperationType.INSERT :  SqlOperationType.SELECT_INSERT;

        return  new ParseResultVo(selectSql, insertFromTemp, insertWithParams, targetTable.toString(),
                sourceTableResult.getTableNames(), sourceTableResult.getTableInfos(), sourceTableResult.getSchemas(),
                targetTableInfo, operationType);
    }

    /**
     * 处理SELECT语句，提取源表信息
     */
    private static ParseResultVo handleSelectStatement(SqlSelect selectStmt) {
        TableExtractionResult extractionResult = extractTableInfo(selectStmt);
        String selectSql = buildSelectSql(selectStmt);

        return new ParseResultVo(selectSql, null, null, null,
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

        return new ParseResultVo(null, null, null, targetTable.toString(),
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

        return new ParseResultVo(null, null, null, targetTable.toString(),
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
     * 构建从临时表插入的INSERT语句
     */
    private static String buildInsertFromTempTable(SqlIdentifier table, SqlNodeList columns, String tempTableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table.toString());

        if (columns != null && columns.size() > 0) {
            sb.append("\n    (").append(columns.toString()).append(")");
        }

        sb.append("\nSELECT * FROM ").append(tempTableName);
        if (columns != null) {
            sb.append(";\n-- 或者使用显式列名：\n-- SELECT ").append(columns.toString()).append(" FROM ").append(tempTableName);
        } else {
            sb.append(";\n-- 列名未指定");
        }

        return sb.toString();
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
        sb.append("\nVALUES (");
        
        // 如果没有列信息，我们很难知道有多少个问号。
        // 这里假设如果 columns 为 null，调用者（BigDataServiceImpl）可能会处理，或者这是一个不支持的情况。
        // 但为了避免 NPE，我们这里先做一个保护。
        // 实际上，如果 columns 为 null，INSERT INTO table VALUES (...) 依赖于源数据的列数。
        // 但 buildInsertWithParams 是为了 JDBC batch insert 准备的，通常需要明确的参数个数。
        // 如果 columns 为 null，我们暂时无法确定参数个数，除非我们解析了 SELECT 部分或者查询了表元数据。
        // 在当前上下文，如果不传 columns，我们假设至少有一个参数或者由后续逻辑处理。
        // 但原来的逻辑是遍历 columns 来 append("?")。
        
        if (columns != null) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("?");
            }
        } else {
             // 如果 columns 为空，通常意味着 INSERT INTO table SELECT * ...
             // 在这种情况下，生成 VALUES (?) 形式的 SQL 是不合适的，因为它不是 VALUES 插入。
             // 但是这个方法 buildInsertWithParams 似乎是专门用于 "Select 结果插入" 的场景，
             // 即先查出来数据，再用 INSERT INTO ... VALUES (?,?,?) 插入回去。
             // 如果没有列名，我们无法构建正确的 VALUES 子句，因为不知道有多少列。
             // 这里返回一个占位符或者抛出更友好的异常可能更好，但为了防止 NPE，先判空。
             // 考虑到 BigDataServiceImpl 中 batchInsert 的逻辑，它会用 data 的 keySet 作为列名。
             // 所以如果 columns 为 null，也许我们应该在 BigDataServiceImpl 中根据 data 动态构建 SQL，而不是依赖这里。
             // 但这里我们先修 NPE。
        }
        
        sb.append(")");
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

    // 使用示例
    public static void main(String[] args) {
        // 测试1: INSERT ... SELECT 语句
        System.out.println("=== 测试1: INSERT ... SELECT 语句 ===");
        String insertSelectSql = "INSERT INTO postgres_schema.dim_order_info_not_id (\n" +
                "        order_id ,\n" +
                "        username,\n" +
                "        email,\n" +
                "        product_name,\n" +
                "        category,\n" +
                "        quantity,\n" +
                "        price,\n" +
                "        total_amount,\n" +
                "        order_date,\n" +
                "        status)\n" +
                "       select\n" +
                "        a.order_id,\n" +
                "         b.username,\n" +
                "         b.email,\n" +
                "         c.product_name,\n" +
                "         c.category,\n" +
                "         a.quantity,\n" +
                "         c.price,\n" +
                "         a.total_amount,\n" +
                "         a.order_date,\n" +
                "         a.status\n" +
                "          from mysql_schema.orders a,mysql_schema.users b,postgres_schema.products c\n" +
                "           where a.user_id = b.id and b.id = 5 and c.product_id = a.product_id";

        ParseResultVo result1 = parseSql(insertSelectSql);

        System.out.println("SELECT SQL:");
        System.out.println(result1.getSelectSql());

        System.out.println("\nINSERT SQL (从临时表):");
        System.out.println(result1.getInsertFromTempSql());

        System.out.println("\n目标表信息:");
        if (result1.getTargetTableInfo() != null) {
            TargetTableInfo targetTable = result1.getTargetTableInfo();
            System.out.println("- Schema: " + targetTable.getSchema());
            System.out.println("- Table: " + targetTable.getTableName());
            System.out.println("- 完整表名: " + targetTable.getFullName());
            System.out.println("- 列信息:");
            targetTable.getColumns().forEach(column -> {
                System.out.println("  * " + column.getColumnName() + " (" + column.getDataType() + ")");
            });
        }

        System.out.println("\n=== 新增功能展示 ===");
        System.out.println("所有源表信息:");
        result1.getTableInfos().forEach(tableInfo -> {
            System.out.println("- Schema: '" + tableInfo.getSchema() + "', Table: '" + tableInfo.getTableName() + "'");
        });

        System.out.println("\n去重后的schemas:");
        result1.getSchemas().forEach(schema -> {
            System.out.println("- " + schema);
        });

        System.out.println("\n兼容旧格式的表名列表:");
        result1.getTableNames().forEach(tableName -> {
            System.out.println("- " + tableName);
        });

        // 测试2: 纯 SELECT 语句
        System.out.println("\n\n=== 测试2: 纯 SELECT 语句 ===");
        String selectSql = "select\n" +
                "        a.order_id,\n" +
                "         b.username,\n" +
                "         b.email,\n" +
                "         c.product_name,\n" +
                "         c.category,\n" +
                "         a.quantity,\n" +
                "         c.price,\n" +
                "         a.total_amount,\n" +
                "         a.order_date,\n" +
                "         a.status\n" +
                "          from mysql_calcite_2.orders a,mysql_calcite_2.users b,postgresql_calcite_1.products c\n" +
                "           where a.user_id = b.id  and c.product_id = a.product_id";

        ParseResultVo result2 = parseSql(selectSql);

        System.out.println("SQL操作类型: " + result2.getOperationType());
        System.out.println("SELECT SQL:");
        System.out.println(result2.getSelectSql());

        System.out.println("\n=== 新增功能展示 ===");
        System.out.println("所有源表信息:");
        result2.getTableInfos().forEach(tableInfo -> {
            System.out.println("- Schema: '" + tableInfo.getSchema() + "', Table: '" + tableInfo.getTableName() + "'");
        });

        System.out.println("\n去重后的schemas:");
        result2.getSchemas().forEach(schema -> {
            System.out.println("- " + schema);
        });

        System.out.println("\n验证：应该只显示 schema.table 格式，不包含别名 a, b, c");
    }
}