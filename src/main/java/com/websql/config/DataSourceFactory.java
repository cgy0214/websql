package com.websql.config;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.stat.DruidDataSourceStatManager;
import com.alibaba.druid.util.JdbcUtils;
import com.websql.model.DataSourceModel;
import com.websql.service.DriverCustomService;
import com.websql.util.SpringContextHolder;
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

    //todo 数据源唯一标识方案待大数据版本开发实现。
    private static final ConcurrentHashMap<String, String> DATA_KEY_IDENTIFIER_MAP = new ConcurrentHashMap<String, String>(0);

    /**
     * 初始化数据源连接池
     *
     * @param dataSourceModels 数据源配置
     */
    @Primary
    public static void initDataSource(List<DataSourceModel> dataSourceModels) {
        DruidDataSource ds;
        int index = 0;
        DriverCustomService driverCustomService = SpringContextHolder.getBean(DriverCustomService.class);
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
            if (ObjectUtil.notEqual(config.getDriverTypeName(), "内置")) {
                ds.setDriverClassLoader(driverCustomService.getCustomClassLoader());
            }
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
                if (ObjectUtil.isNotNull(ds)) {
                    DruidDataSourceStatManager.removeDataSource(ds);
                }
            }
        }
        log.info("initialization {} dataSource Successful", index);
    }


    /**
     * 保存数据源连接池
     *
     * @param config 数据源对象
     * @throws SQLException
     */
    public static void saveDataSource(DataSourceModel config) throws SQLException {
        if (ObjectUtil.isNull(config)) {
            throw new RuntimeException("数据源对象为空,无法创建连接池");
        }
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
        if (ObjectUtil.notEqual("内置", config.getDriverTypeName())) {
            DriverCustomService driverCustomService = SpringContextHolder.getBean(DriverCustomService.class);
            ds.setDriverClassLoader(driverCustomService.getCustomClassLoader());
        }
        try {
            ds.init();
            DATA_SOURCE_MAP.put(config.getDbName().trim(), ds);
        } catch (Exception e) {
            DruidDataSourceStatManager.removeDataSource(ds);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获得连接池
     */
    private static DruidDataSource getDataSource(String sourceKey) {
        return DATA_SOURCE_MAP.get(sourceKey.trim());
    }

    /**
     * 从缓存里删除指定的数据源连接池
     */
    public static void removeDataSource(String sourceKey) {
        DruidDataSource dataSource = getDataSource(sourceKey);
        if(ObjectUtil.isNotNull(dataSource)){
            DATA_SOURCE_MAP.remove(sourceKey.trim());
            DruidDataSourceStatManager.removeDataSource(dataSource);
            log.info(" Delete DataSource {} Successful.", sourceKey);
        }
    }


    /**
     * 获得数据库连接
     *
     * @param sourceKey 数据源key
     * @return
     */
    public static Connection getConnectionBySourceKey(String sourceKey) {
        DruidDataSource dataSource = getDataSource(sourceKey);
        if (dataSource == null) {
            String errorMsg = String.format("%s: 连接销毁, 未获取到数据源", sourceKey);
            log.error("{},需在参数设置中重新建立连接。", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        try {
            Connection connection = dataSource.getConnection();
            if (connection == null) {
                log.warn("从数据源 {} 获取连接失败，返回了 null,连接池会尝试重新连接.", sourceKey);
                throw new SQLException("无法从数据源获取有效连接: " + sourceKey);
            }
            return connection;
        } catch (SQLException e) {
            log.error("从数据源 {} 获取连接时发生异常: {}", sourceKey, e.getMessage(), e);
            throw new RuntimeException("获取数据库连接失败: " + sourceKey, e);
        }
    }

    /**
     * 获取数据库类型
     *
     * @param sourceKey 数据源key
     * @return
     */
    public static String getDbType(String sourceKey) {
        DruidDataSource druidDataSource = getDataSource(sourceKey);
        if (ObjectUtil.isNotNull(druidDataSource)) {
            return getDbTypeByJdbcUrl(druidDataSource.getRawJdbcUrl(), "").name();
        }
        return null;
    }

    /**
     * 获取数据库类型
     *
     * @param jdbcUrl   jdbcUrl
     * @param className 类名
     * @return
     */
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
     * @param sourceKey 数据源key
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

    /**
     * 获取数据库产品名称
     *
     * @param sourceKey 数据源key
     * @return
     */
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
     * 根据jdbcUrl获取数据库实例名称
     *
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
