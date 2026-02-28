package com.websql.service;

import com.websql.model.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @ClassName DbSourceService
 * @Description 数据源接口
 * @Author rabbit boy_0214@sina.com
 * @Date 2019/6/14 0014 17:33
 **/
public interface DbSourceService {

    List<DataSourceModel> reloadDataSourceList();

    Result<DataSourceModel> selectDbSourceList(DataSourceModel model);

    void addDbSource(DataSourceModel model,Long id);

    List<Map<String, String>> dbsourceSqlList(DataSourceModel model);

    void saveSqlText(DbSqlText model);

    List<Map<String, String>> sqlTextList(DataSourceModel model);

    Result<DbSqlText> getDbSqlText(DbSqlText model);

    void deleteDataBaseSource(Long id);

    SysSetup initSysSetup();

    void deleteSqlText(String id);

    Result<SysLog> getLogList(SysLog model);

    void delSysLog();

    void delUserLog();

    Integer selectDbByName(String dbName);
    
    Integer selectDbByIdentifier(String sourceIdentifier);

    AjaxResult findTableField(String database);

    void updateDataSourceName(Long id, String name) throws SQLException;

    AjaxResult findMetaTable(String database, String table);

    AjaxResult showTableSql(String database, String table);

    void sqlTextDeleteAll();

    AjaxResult executeSqlNew(ExecuteSql sql);

    DataSourceModel selectDbById(Long id);

    List<DbSqlText> sqlTextListAll();

    void deleteDataSourceAll();

    Map<String, Object> createAsyncExport(ExecuteSql executeSql);
    
    void deleteSqlTextByDataSourceCode(String dataSourceCode);
    
    int countSqlTextByDataSourceCode(String dataSourceCode);
    
    List<Map<String, String>> sqlTextListByDataSource(DataSourceModel model, String dataSourceCode);

    SysExportModel exportAsyncData(Long id);

    Result<SysExportModel> exportFilesLogList(SysExportModel model);

    List<MetaTreeTable> metaTreeTableList();

    /**
     * 获取数据库列表（只返回数据库节点，不加载表）
     * @return 数据库节点列表
     */
    List<MetaTreeTable> metaDatabaseList();

    /**
     * 根据数据库名获取表列表
     * @param database 数据库名
     * @return 表节点列表
     */
    List<MetaTreeTable> metaTableListByDatabase(String database);

    /**
     * 搜索数据库和表（模糊搜索）
     * @param keyword 搜索关键词
     * @return 匹配的树节点列表
     */
    List<MetaTreeTable> searchMetaTree(String keyword);

}
