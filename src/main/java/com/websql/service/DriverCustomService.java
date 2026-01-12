package com.websql.service;

import com.websql.model.*;

import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface DriverCustomService {

    void systemLoadConfigDriver();

    void loadDriverCustomAll();

    URLClassLoader getCustomClassLoader();

    List<Map<String, String>> findDriverConfigListSelect(String id);

    Result<SysDriverConfig> selectDriverConfigList(SysDriverConfig sysDriverConfig);

    AjaxResult deleteDriverConfig(Long id);

    AjaxResult saveOrUpdateDriverConfig(SysDriverConfig sysDriverConfig);


    List<SysDriverConfig> findDriverConfigList(String id);

    Connection getDriverConnection(DataSourceModel model) throws SQLException;

    String downloadDriver(DriverDependencyQo qo);

}
