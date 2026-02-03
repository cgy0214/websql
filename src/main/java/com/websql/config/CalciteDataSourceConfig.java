package com.websql.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @ClassName CalciteDataSourceConfig
 * @Description 跨库管理
 * @Author rabbit boy_0214@sina.com
 * @Date 2026/1/30 17:14
 **/
@Component
public class CalciteDataSourceConfig {

    public Connection createConnection(DruidDataSource dataSources1,DruidDataSource dataSource2) throws SQLException {

        Properties info = new Properties();
        info.setProperty("lex", "JAVA");
        info.setProperty("timeZone", "UTC");
        Connection calciteConnection = DriverManager.getConnection("jdbc:calcite:", info);

        CalciteConnection calciteConn = calciteConnection.unwrap(CalciteConnection.class);
        SchemaPlus rootSchema = calciteConn.getRootSchema();

        Schema mysqlSchema = JdbcSchema.create(
                rootSchema,
                "mysql_calcite_2",
                dataSources1,
                "calcite",
                null
        );
        Schema postgresSchema = JdbcSchema.create(
                rootSchema,
                "postgresql_calcite_1",
                dataSource2,
                "calcite",
                "public"
        );
        rootSchema.add("mysql_calcite_2", mysqlSchema);
        rootSchema.add("postgresql_calcite_1", postgresSchema);
        return calciteConnection;
    }
}
