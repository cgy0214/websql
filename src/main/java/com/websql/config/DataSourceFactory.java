package com.websql.config;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.stat.DruidDataSourceStatManager;
import com.alibaba.druid.util.JdbcUtils;
import com.websql.model.DataSourceModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName DataSourceFactory
 * @Description 数据源管理
 * @Author rabbit boy_0214@sina.com
 * @Date 2019/6/14 0014 16:29
 **/
@Slf4j
public class DataSourceFactory {

    private static final ConcurrentHashMap<String, DruidDataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<String, DruidDataSource>(1);

    /**
     * 根据jdbc参数获得数据源连接池dbcp，并放入ConcurrentHashMap
     */
    @Primary
    public static void initDataSource(List<DataSourceModel> dataSourceModels) {
        DruidDataSource ds = null;
        int index = 0;
        for (DataSourceModel config : dataSourceModels) {
            removeDataSource(config.getDbName());
            ds = new DruidDataSource();
            ds.setDriverClassName(config.getDriverClass().trim());
            ds.setUsername(config.getDbAccount().trim());
            ds.setPassword(config.getDbPassword().trim());
            ds.setUrl(config.getDbUrl().trim());
            ds.setInitialSize(config.getInitialSize());
            ds.setMaxActive(config.getMaxActive());
            ds.setMaxWait(config.getMaxWait());
            ds.setValidationQuery(config.getDbCheckUrl());
            ds.setBreakAfterAcquireFailure(false);
            ds.setConnectionErrorRetryAttempts(1);
            ds.setNotFullTimeoutRetryCount(0);
            ds.setTimeBetweenConnectErrorMillis(30000);
            try {
                //druid不支持某些数据库防火墙功能
                DbType dbTypeRaw = getDbTypeByJdbcUrl(config.getDbUrl().trim(), config.getDriverClass().trim());
                if (dbTypeRaw.equals(DbType.dm)
                        || dbTypeRaw.equals(DbType.kingbase)
                        || dbTypeRaw.equals(DbType.oscar)
                        || dbTypeRaw.equals(DbType.odps)
                        || dbTypeRaw.equals(DbType.clickhouse)) {
                    //ds.setFilters("stat");
                } else {
                    ds.setFilters("stat,wall");
                }
                ds.setConnectionProperties("druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000");
                ds.setAsyncInit(true);
                ds.init();
                DATA_SOURCE_MAP.put(config.getDbName().trim(), ds);
                index++;
            } catch (SQLException e) {
                log.error("数据源初始化失败:{},错误信息：{}", config.getDbName(), e.getMessage());
                DruidDataSourceStatManager.removeDataSource(ds);
            }
        }
        log.info("Successful initialization  " + index + " DbSources ");
    }


    /**
     * 插入数据源连接池到缓存
     */
    public static void saveDataSource(DataSourceModel config) throws SQLException {
        if (config != null) {
            DruidDataSource ds = new DruidDataSource();
            ds.setDriverClassName(config.getDriverClass().trim());
            ds.setUsername(config.getDbAccount().trim());
            ds.setPassword(config.getDbPassword().trim());
            ds.setUrl(config.getDbUrl().trim());
            ds.setInitialSize(config.getInitialSize());
            ds.setMaxActive(config.getMaxActive());
            ds.setMaxWait(config.getMaxWait());
            ds.setValidationQuery(config.getDbCheckUrl());
            ds.setBreakAfterAcquireFailure(true);
            ds.setConnectionErrorRetryAttempts(0);
            ds.setFailFast(true);
            try {
                ds.init();
                DATA_SOURCE_MAP.put(config.getDbName().trim(), ds);
            } catch (Exception e) {
                DruidDataSourceStatManager.removeDataSource(ds);
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * 从缓存里删除指定的数据源连接池
     */
    public static void removeDataSource(String sourceKey) {
        DruidDataSource ds = DATA_SOURCE_MAP.get(sourceKey.trim());
        if (ds == null) {
            return;
        }
        //shutdownDataSource(ds);
        DATA_SOURCE_MAP.remove(sourceKey.trim());
        DruidDataSourceStatManager.removeDataSource(ds);
        log.info(" Delete DataSource {} Successful.", sourceKey);
    }


    /**
     * 修改数据源连接池到缓存,未实现修改方法
     */

    public static void updateDataSource(DataSourceModel config) throws Exception {
        if (config != null) {
            //先将原有数据源连接池从缓存中删除
            removeDataSource(config.getDbName());
            DruidDataSource ds = new DruidDataSource();
            ds.setDriverClassName(config.getDriverClass().trim());
            ds.setUsername(config.getDbAccount().trim());
            ds.setPassword(config.getDbPassword().trim());
            ds.setUrl(config.getDbUrl().trim());
            ds.setInitialSize(config.getInitialSize());
            ds.setMaxActive(config.getMaxActive());
            ds.setMaxWait(config.getMaxWait());
            ds.setValidationQuery("select 1");
            ds.setConnectionErrorRetryAttempts(1);
            ds.setNotFullTimeoutRetryCount(-1);
            ds.setBreakAfterAcquireFailure(true);
            ds.setConnectionErrorRetryAttempts(0);
            DATA_SOURCE_MAP.put(config.getDbName().trim(), ds);
        }
        log.info("Successful Update  DbSources ");
    }


    /**
     * 获得连接池
     */

    public static DruidDataSource getDataSource(String sourceKey) {
        return DATA_SOURCE_MAP.get(sourceKey.trim());
    }

    /**
     * 获取连接
     *
     * @param source
     * @return
     */
    public static Connection getConnection(DruidDataSource source) {
        Connection con = null;
        if (source != null) {
            try {
                con = source.getConnection();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return con;
        }
        return con;
    }

    protected static void shutdownDataSource(DruidDataSource source) {
        if (source != null) {
            source.close();
        }
    }


    public static String getDbType(String sourceKey) {
        DruidDataSource druidDataSource = DATA_SOURCE_MAP.get(sourceKey);
        if (ObjectUtil.isNotNull(druidDataSource)) {
            return getDbTypeByJdbcUrl(druidDataSource.getRawJdbcUrl(), "").name();
        }
        return null;
    }

    public static DbType getDbTypeByJdbcUrl(String jdbcUrl, String className) {
        DbType dbType = JdbcUtils.getDbTypeRaw(jdbcUrl, className);
        if (ObjectUtil.isNotEmpty(dbType)) {
            return dbType;
        }
        //druid对神通缺失判断
        String jdbcOscar = "jdbc:oscar:";
        if (jdbcUrl.startsWith(jdbcOscar)) {
            return DbType.oscar;
        }
        throw new RuntimeException("根据url[" + jdbcUrl + "]无法找到对应的数据库类型!");
    }

    /**
     * 获取数据库名称
     *
     * @param sourceKey
     * @return
     */
    public static String getDataBaseName(String sourceKey) {
        try {
            DruidDataSource dataSource = getDataSource(sourceKey);
            try (Connection conn = dataSource.getConnection()) {
                return conn.getCatalog();
            }
        } catch (Exception e) {
            log.error("获取数据库名称失败,{}", e.getMessage());
        }
        return null;
    }

    public static String getDataBaseProductName(String sourceKey) {
        try {
            DruidDataSource dataSource = getDataSource(sourceKey);
            try (Connection conn = dataSource.getConnection()) {
                return conn.getMetaData().getDatabaseProductName();
            }
        } catch (Exception e) {
            log.error("获取数据库产品名称失败,{}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据jdbcUrl获取数据库名称
     * @param jdbcUrl url
     * @return
     */
    public static String getDataBaseNameByJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            return null;
        }
        try {
            Pattern pattern = Pattern.compile(".*://[^/]+(?::\\d+)?/([^/?]+)");
            Matcher matcher = pattern.matcher(jdbcUrl);
            if (matcher.find()) {
                return matcher.group(1);
            }
            pattern = Pattern.compile("@//[^/]+(?::\\d+)?/([^/?]+)");
            matcher = pattern.matcher(jdbcUrl);
            if (matcher.find()) {
                return matcher.group(1);
            }
            if (jdbcUrl.contains(":file:") || jdbcUrl.contains(":mem:")) {
                int colonIndex = jdbcUrl.lastIndexOf(':');
                if (colonIndex != -1) {
                    String path = jdbcUrl.substring(colonIndex + 1);
                    int slashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
                    if (slashIndex != -1) {
                        path = path.substring(slashIndex + 1);
                    }
                    int dotIndex = path.lastIndexOf('.');
                    return (dotIndex != -1) ? path.substring(0, dotIndex) : path;
                }
            }
            int protocolIndex = jdbcUrl.indexOf("://");
            if (protocolIndex > 0) {
                String afterProtocol = jdbcUrl.substring(protocolIndex + 3);
                int queryIndex = afterProtocol.indexOf('?');
                if (queryIndex > 0) {
                    afterProtocol = afterProtocol.substring(0, queryIndex);
                }
                String[] parts = afterProtocol.split("/");
                if (parts.length > 1) {
                    String lastPart = parts[parts.length - 1];
                    int portIndex = lastPart.indexOf(':');
                    if (portIndex > 0 && !lastPart.startsWith(":")) {
                        lastPart = lastPart.substring(portIndex + 1);
                    }
                    return lastPart.split("\\?")[0]; // 再次确保去除参数
                }
            }
        } catch (Exception e) {
            log.error("解析jdbcUrl数据库名称失败,{}", e.getMessage());
        }
        return null;
    }
}
