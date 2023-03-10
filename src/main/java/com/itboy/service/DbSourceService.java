package com.itboy.service;

import com.itboy.model.*;

import java.util.List;
import java.util.Map;

/**
 * @ClassName DbSourceService
 * @Description 数据源接口
 * @Author 超 boy_0214@sina.com
 * @Date 2019/6/14 0014 17:33
 **/
public interface DbSourceService {
    Result<DataSourceModel> selectDbSourceList(DataSourceModel model);

    void addDbSource(DataSourceModel model);

    List<Map<String, String>> dbsourceSqlList(DataSourceModel model);

    void saveSqlText(DbSqlText model);

    List<Map<String, String>> sqlTextList(DataSourceModel model);

    Result<DbSqlText> getDbSqlText(DbSqlText model);

    DataSourceModel delDbSource(String id);

    SysSetup initSysSetup();

    void deleteSqlText(String id);

    void insertLog(SysLog logs);

    Result<SysLog> getLogList(SysLog model);

    void delSysLog();

    void delUserLog();

}
