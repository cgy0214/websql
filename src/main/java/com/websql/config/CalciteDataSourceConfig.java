package com.websql.config;

import cn.hutool.core.util.ObjectUtil;
import com.websql.model.CalciteDataSourceParams;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * @ClassName CalciteDataSourceConfig
 * @Description 跨库管理
 * @Author rabbit boy_0214@sina.com
 * @Date 2026/1/30 17:14
 **/
@Component
public class CalciteDataSourceConfig {

    static {
        System.setProperty("saffron.default.charset", "UTF-8");
    }

    public Connection createConnection(List<CalciteDataSourceParams> dataList) throws SQLException {
        if (ObjectUtil.isNull(dataList) || dataList.isEmpty()) {
            return null;
        }
        Properties info = new Properties();
        info.setProperty("lex", "JAVA");
        info.setProperty("timeZone", "UTC");
        info.setProperty("charset", "UTF-8");
        info.setProperty("saffron.default.charset", "UTF-8");
        Connection calciteConnection = DriverManager.getConnection("jdbc:calcite:", info);

        CalciteConnection calciteConn = calciteConnection.unwrap(CalciteConnection.class);
        SchemaPlus rootSchema = calciteConn.getRootSchema();

        for (CalciteDataSourceParams calciteDataSourceParams : dataList) {
            JdbcSchema itemSchema = JdbcSchema.create(
                    rootSchema,
                    calciteDataSourceParams.getSourceKey(),
                    calciteDataSourceParams.getDataSource(),
                    calciteDataSourceParams.getCatalog(),
                    calciteDataSourceParams.getSchema()
            );
            rootSchema.add(calciteDataSourceParams.getSourceKey(), itemSchema);
        }

        return calciteConnection;
    }

}
