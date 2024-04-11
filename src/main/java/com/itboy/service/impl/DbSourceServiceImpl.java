package com.itboy.service.impl;

import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.itboy.config.DataSourceFactory;
import com.itboy.config.JdbcUtils;
import com.itboy.config.SqlDruidParser;
import com.itboy.config.SqlParserHandler;
import com.itboy.dao.*;
import com.itboy.model.*;
import com.itboy.service.DbSourceService;
import com.itboy.service.TeamSourceService;
import com.itboy.util.CacheUtils;
import com.itboy.util.PasswordUtil;
import com.itboy.util.StpUtils;
import com.itboy.util.TableFieldSqlUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @ClassName DbSourceServiceImpl
 * @Description 业务处理
 * @Author 超 boy_0214@sina.com
 * @Date 2019/6/14 0014 17:34
 **/
@Service
public class DbSourceServiceImpl implements DbSourceService {

    @Resource
    private DbSourceRepository dbSourceRepository;

    @Resource
    private DbSqlTextRepository dbSqlTextRepository;

    @Resource
    private SysSetUpRepository sysSetUpRepository;

    @Resource
    private SysLogRepository sysLogRepository;

    @Resource
    private SysUserLogRepository sysUserLogRepository;

    @Resource
    private TeamSourceService teamSourceService;


    @Override
    public List<DataSourceModel> reloadDataSourceList() {
        return dbSourceRepository.reloadDataSourceList();
    }

    @Override
    public Result<DataSourceModel> selectDbSourceList(DataSourceModel result) {
        Result<DataSourceModel> result1 = new Result<DataSourceModel>();
        PageRequest pageRequest = PageRequest.of(result.getPage() - 1, result.getLimit());
        List<TeamResourceModel> resourceModels = teamSourceService.queryTeamResourceByTeamId(Collections.singletonList(Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId()), "DATASOURCE");
        Specification<DataSourceModel> spec = (root, query, cb) -> {
            Path<String> dbName = root.get("dbName");
            Path<String> dbAccount = root.get("dbAccount");
            Path<String> dbUrl = root.get("dbUrl");
            String dbUrl1 = result.getDbUrl() == null ? "" : result.getDbUrl();
            String account1 = result.getDbAccount() == null ? "" : result.getDbAccount();
            String dbname2 = result.getDbName() == null ? "" : result.getDbName();
            Predicate p1 = cb.like(dbName, "%" + dbname2 + "%");
            Predicate p2 = cb.like(dbAccount, "%" + account1 + "%");
            Predicate p3 = cb.like(dbUrl, "%" + dbUrl1 + "%");
            Predicate p = cb.and(p1, p2, p3, root.get("id").in(resourceModels.stream().map(TeamResourceModel::getResourceId).collect(Collectors.toList())));
            query.orderBy(cb.desc(root.get("id")));
            return p;
        };
        Page<DataSourceModel> all = dbSourceRepository.findAll(spec, pageRequest);
        result1.setList(all.getContent());
        result1.setCount((int) all.getTotalElements());
        return result1;
    }

    @Override
    public Result<DbSqlText> getDbSqlText(DbSqlText model) {
        Result<DbSqlText> result1 = new Result<DbSqlText>();
        PageRequest request = PageRequest.of(model.getPage() - 1, model.getLimit());
        Long teamId = Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId();
        Specification<DbSqlText> spec = (root, query, cb) -> {
            Path<String> title = root.get("title");
            Path<String> sqlText = root.get("sqlText");
            String sqlText1 = model.getSqlText() == null ? "" : model.getSqlText();
            String title1 = model.getTitle() == null ? "" : model.getTitle();
            Predicate p2 = cb.like(sqlText, "%" + sqlText1 + "%");
            Predicate p3 = cb.like(title, "%" + title1 + "%");
            Predicate p = cb.and(p2, p3, root.get("teamId").in(teamId));
            query.orderBy(cb.desc(root.get("id")));
            return p;
        };
        Page<DbSqlText> all = dbSqlTextRepository.findAll(spec, request);
        result1.setList(all.getContent());
        result1.setCount((int) all.getTotalElements());
        return result1;
    }

    @Override
    public void deleteDataBaseSource(Long id) {
        CacheUtils.remove("data_source_model");
        DataSourceModel dataSourceModel = dbSourceRepository.selectById(id);
        dbSourceRepository.deleteById(id);
        DataSourceFactory.removeDataSource(dataSourceModel.getDbName());
        teamSourceService.deleteResourceByResIds(Collections.singletonList(Long.valueOf(id)), "DATASOURCE");
    }

    @Override
    public SysSetup initSysSetup() {
        List<SysSetup> sysList = sysSetUpRepository.findAll();
        if (sysList.size() > 0) {
            return sysList.get(0);
        } else {
            return SysSetup.getInstance();
        }
    }

    @Override
    public void deleteSqlText(String id) {
        dbSqlTextRepository.delsqlText(Integer.valueOf(id));
    }

    @Override
    public Result<SysLog> getLogList(SysLog model) {
        Result<SysLog> result = new Result<SysLog>();
        Specification<SysLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>(3);
            if (ObjectUtil.isNotEmpty(model.getLogContent())) {
                predicates.add(cb.like(root.get("name"), "%" + model.getLogContent() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getLogContent())) {
                predicates.add(cb.like(root.get("logName"), "%" + model.getLogName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getLogDbSource())) {
                predicates.add(cb.like(root.get("logDbSource"), "%" + model.getLogDbSource() + "%"));
            }
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
        Page<SysLog> all = sysLogRepository.findAll(spec, PageRequest.of(model.getPage() - 1, model.getLimit()));
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public void delSysLog() {
        sysLogRepository.deleteAll();
    }

    @Override
    public void delUserLog() {
        sysUserLogRepository.deleteAll();
    }

    /**
     * 此方法执行存在性能问题，后期3.X版本未来将会移除。替代方法使用executeSqlNew
     *
     * @param sql
     * @return
     */
    @Override
    public Map executeSql(ExecuteSql sql) {
        Map result = new HashMap(3);
        result.put("code", 1);
        SysUser user = StpUtils.getCurrentUser();
        String userName = user.getUserId() + ":" + user.getUserName();
        SysLog log = new SysLog("sql执行记录", "1", sql.getDataBaseName(), sql.getSqlText(), userName, DateUtil.now());
        try {
            if (ObjectUtil.isEmpty(sql.getDataBaseName()) || ObjectUtil.isEmpty(sql.getSqlText())) {
                throw new NullPointerException("请选择数据源或编写SQL!");
            }
            Map<String, Object> sqlParser = SqlDruidParser.sqlParser(sql.getDataBaseName(), sql.getSqlText());
            if (sqlParser.get("executeType") == null) {
                throw new NullPointerException("SQL解析异常");
            }
            result.put("sqlExecuteType", sqlParser.get("executeType"));
            result.put("tableName", sqlParser.get("tableName"));
            List<String> executeSqlList = (List<String>) sqlParser.get("executeSql");
            List dataList = new ArrayList();
            if (sqlParser.get("executeType").equals("SELECT") || sqlParser.get("executeType").equals("DESC")) {
                for (String executeSql : executeSqlList
                ) {
                    Map<String, Object> resultData = JdbcUtils.findMoreResult(sql.getDataBaseName(), executeSql, new ArrayList<>());
                    dataList.add(resultData);
                }
            } else {
                for (String executeSql : executeSqlList
                ) {
                    Map<String, Object> resultData = JdbcUtils.updateByPreparedStatement(sql.getDataBaseName(), executeSql, new ArrayList<>());
                    dataList.add(resultData);
                }
            }
            log.setLogResult(dataList.toString());
            result.put("dataList", dataList);
        } catch (Exception e) {
            log.setLogResult(e.getMessage());
            e.printStackTrace();
            result.put("code", 2);
            result.put("msg", e.getMessage());
        } finally {
            SysSetup sysSetup = CacheUtils.get("sys_setup", SysSetup.class);
            if (sysSetup.getEnabledSqlLog() == 1) {
                sysLogRepository.save(log);
            }
        }
        return result;
    }

    @Override
    public AjaxResult executeSqlNew(ExecuteSql sql) {
        SysLog log = new SysLog("sql执行记录", "1", sql.getDataBaseName(), Base64Encoder.encode(sql.getSqlText()), StpUtils.getUserExtName(), DateUtil.now());
        try {
            List<SqlParserVo> parserVoList = SqlParserHandler.getParserVo(sql.getDataBaseName(), sql.getSqlText());
            if (parserVoList.isEmpty()) {
                return AjaxResult.error("解析SQL失败,返回为空请重试!");
            }
            List<SqlExecuteResultVo> resultVos = new ArrayList<>(parserVoList.size());
            for (SqlParserVo sqlParserVo : parserVoList) {
                TimeInterval timer = DateUtil.timer();
                SqlExecuteResultVo vo = new SqlExecuteResultVo();
                vo.setTableNameList(JSON.toJSONString(sqlParserVo.getTableNameList()));
                vo.setExecuteType(sqlParserVo.getMethodType());
                vo.setParserSql(sqlParserVo.getSqlContent());
                vo.setDataBaseKey(sql.getDataBaseName());
                if (SqlParserHandler.SELECT.equals(sqlParserVo.getMethodType())) {
                    Map<String, Object> moreResult = JdbcUtils.findMoreResult(sql.getDataBaseName(), sqlParserVo.getSqlContent(), new ArrayList<>());
                    vo.setData(moreResult.get("data"));
                    vo.setStatus(Integer.parseInt(moreResult.get("code").toString()));
                    vo.setType(0);
                    vo.setErrorMessage(moreResult.get("msg").toString());
                } else {
                    Map<String, Object> moreResult = JdbcUtils.updateByPreparedStatement(sql.getDataBaseName(), sqlParserVo.getSqlContent(), new ArrayList<>());
                    vo.setType(1);
                    vo.setData(moreResult.get("data"));
                    vo.setStatus(Integer.parseInt(moreResult.get("code").toString()));
                    vo.setErrorMessage(moreResult.get("msg").toString());
                }
                vo.setTime(timer.intervalRestart());
                resultVos.add(vo);
            }
            log.setLogResult(JSON.toJSONString(resultVos));
            return AjaxResult.success(resultVos);
        } catch (Exception e) {
            e.printStackTrace();
            log.setLogResult(e.getMessage());
            return AjaxResult.error(e.getMessage());
        } finally {
            SysSetup sysSetup = CacheUtils.get("sys_setup", SysSetup.class);
            if (sysSetup.getEnabledSqlLog() == 1) {
                sysLogRepository.save(log);
            }
        }
    }


    @Override
    public Integer selectDbByName(String paramName) {
        return dbSourceRepository.findDataSourceByName(paramName);
    }

    @Override
    public void addDbSource(DataSourceModel model, Long teamId) {
        if (selectDbByName(model.getDbName()) > 0) {
            throw new RuntimeException("数据源名称已经存在,请换一个!");
        }
        try {
            DataSourceFactory.saveDataSource(model);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CacheUtils.remove("data_source_model");
        if (ObjectUtil.isNotEmpty(model.getDbPassword())) {
            String encrypt = PasswordUtil.encrypt(model.getDbPassword());
            model.setDbPassword(encrypt);
        }
        if (ObjectUtil.isNotEmpty(model.getDbAccount())) {
            String encrypt = PasswordUtil.encrypt(model.getDbAccount());
            model.setDbAccount(encrypt);
        }
        DataSourceModel save = dbSourceRepository.save(model);
        //兼容系统初始化时获取不到teamId问题
        Long tid = teamId == null ? StpUtils.getCurrentActiveTeam().getId() : teamId;
        teamSourceService.updateTeamResources(Collections.singletonList(tid.toString()), Collections.singletonList(save.getId()), "DATASOURCE");

    }

    @Override
    public List<Map<String, String>> dbsourceSqlList(DataSourceModel model) {
        List<Map<String, String>> dataSourceList = CacheUtils.get("data_source_model", List.class);
        if (ObjectUtil.isNull(dataSourceList)) {
            dataSourceList = new ArrayList<>(0);
            List<DataSourceModel> list = dbSourceRepository.findAll().stream().filter(s -> s.getDbState().equals("有效")).sorted(Comparator.comparing(DataSourceModel::getId).reversed()).collect(Collectors.toList());
            for (DataSourceModel dataSourceModel : list) {
                Map<String, String> item = new HashMap<>(3);
                item.put("code", dataSourceModel.getDbName());
                item.put("value", dataSourceModel.getDbName());
                item.put("id", dataSourceModel.getId().toString());
                item.put("select", "false");
                dataSourceList.add(item);
            }
            CacheUtils.put("data_source_model", dataSourceList);
        }
        //按照团队过滤
        Long teamId = StpUtils.getCurrentActiveTeam().getId();
        List<String> resourceIds = teamSourceService.queryTeamResourceByTeamId(Collections.singletonList(teamId), "DATASOURCE").stream().map(s -> s.getResourceId().toString()).collect(Collectors.toList());
        dataSourceList = dataSourceList.stream().filter(s -> resourceIds.contains(s.get("id"))).collect(Collectors.toList());
        //按最近使用的数据源推荐并选中此数据源
        String source = sysLogRepository.querySysLogDataSource();
        dataSourceList.stream().filter(s -> s.get("code").equals(source)).forEach(s -> s.put("short", "1"));
        dataSourceList.stream().filter(s -> !s.get("code").equals(source)).forEach(s -> s.put("short", "2"));
        dataSourceList.sort(Comparator.comparingInt((Map o) -> Integer.valueOf(o.get("short").toString())));
        if (!dataSourceList.isEmpty()) {
            dataSourceList.stream().skip(1).forEach(s -> s.put("select", "false"));
            dataSourceList.get(0).put("select", "true");
        }
        return dataSourceList;
    }

    @Override
    public void saveSqlText(DbSqlText model) {
        model.setSqlCreateUser(StpUtils.getCurrentUserName());
        if (ObjectUtil.isNull(model.getTeamId())) {
            model.setTeamId(Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId());
        }
        dbSqlTextRepository.save(model);
    }

    @Override
    public void sqlTextDeleteAll() {
        dbSqlTextRepository.deleteAll();
    }

    @Override
    public List<Map<String, String>> sqlTextList(DataSourceModel model) {
        List<Map<String, String>> sqlTextModelList = new ArrayList<>();
        Long itemId = Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId();
        List<DbSqlText> all = dbSqlTextRepository.findAll().stream().filter(s -> s.getTeamId().equals(itemId)).sorted(Comparator.comparing(DbSqlText::getId).reversed()).collect(Collectors.toList());
        for (DbSqlText dbSqlText : all) {
            Map<String, String> item = new HashMap<>(2);
            item.put("code", dbSqlText.getSqlText());
            item.put("value", dbSqlText.getTitle());
            sqlTextModelList.add(item);
        }
        return sqlTextModelList;
    }

    @Override
    public AjaxResult findTableField(String database) {
        SysSetup sysSetup = CacheUtils.get("sys_setup", SysSetup.class);
        if (ObjectUtil.isNull(sysSetup) || ObjectUtil.notEqual(sysSetup.getEnabledHint(), 0)) {
            return AjaxResult.success("系统关闭提示功能，可以再参数中设置开启。");
        }
        String key = "table_field_list" + database + StpUtils.getCurrentActiveTeam().getId();
        Map<String, List<String>> resultMap = CacheUtils.get(key, Map.class);
        if (ObjectUtil.isNull(resultMap)) {
            String viewSql = TableFieldSqlUtils.getViewSql(database);
            if (ObjectUtil.isEmpty(viewSql)) {
                return AjaxResult.error("不兼容的数据库类型!");
            }
            Map<String, Object> map = JdbcUtils.findMoreResult(database, viewSql, new ArrayList<>());
            if (ObjectUtil.notEqual(map.get("code"), "1")) {
                return AjaxResult.error("查询此数据库表名时失败!");
            }
            List<Map> list = (List<Map>) map.get("data");
            resultMap = list.stream().collect(Collectors.groupingBy(s -> s.get("TABLE_NAME").toString(), Collectors.mapping(s -> s.get("TABLE_FIELD").toString(), Collectors.toList())));
            CacheUtils.put(key, resultMap);
        }
        return AjaxResult.success(resultMap);
    }

    @Override
    public void updateDataSourceName(Long id, String name) throws SQLException {
        DataSourceModel dataSourceModel = dbSourceRepository.selectById(id);
        dataSourceModel.setDbName(name);
        dbSourceRepository.save(dataSourceModel);
        CacheUtils.remove("data_source_model");
        if (ObjectUtil.isNotEmpty(dataSourceModel.getDbPassword()) && ObjectUtil.isNotEmpty(dataSourceModel.getDbAccount())) {
            String account = PasswordUtil.decrypt(dataSourceModel.getDbAccount());
            dataSourceModel.setDbAccount(account);
            String password = PasswordUtil.decrypt(dataSourceModel.getDbPassword());
            dataSourceModel.setDbPassword(password);
            DataSourceFactory.initDataSource(Collections.singletonList(dataSourceModel));
        }
    }

    @Override
    public AjaxResult findMetaTable(String database, String table) {
        String key = "data_source_meta_" + database + "_" + table;
        DataSourceMeta dataSourceMeta = CacheUtils.get(key, DataSourceMeta.class);
        if (ObjectUtil.isNotNull(dataSourceMeta)) {
            return AjaxResult.success(dataSourceMeta);
        }
        try {
            Future<DataSourceMeta> tableMetaFuture = ThreadUtil.execAsync(() -> JdbcUtils.getTableMeta(database, table));
            Future<List<DataSourceTableMeta>> columnsMetaFuture = ThreadUtil.execAsync(() -> JdbcUtils.getColumnsMeta(database, table));
            Future<List<DataSourceIndexMeta>> indexInfoMetaFuture = ThreadUtil.execAsync(() -> JdbcUtils.getIndexInfoMeta(database, table));
            Future<List<DataSourceTableMeta>> keyMetaFuture = ThreadUtil.execAsync(() -> JdbcUtils.getKeyMeta(database, table));
            DataSourceMeta tableMeta = tableMetaFuture.get();
            if (ObjectUtil.isNull(tableMeta)) {
                columnsMetaFuture.cancel(true);
                indexInfoMetaFuture.cancel(true);
                keyMetaFuture.cancel(true);
                return AjaxResult.error("数据库中没有[" + table + "]信息!");
            }
            dataSourceMeta = JdbcUtils.getDataSourceMeta(database, table);
            dataSourceMeta.setDatabaseName(tableMeta.getDatabaseName());
            dataSourceMeta.setTableType(tableMeta.getTableType());
            dataSourceMeta.setTableComment(tableMeta.getTableComment());
            dataSourceMeta.setTableSchema(tableMeta.getTableSchema());
            List<DataSourceTableMeta> tableMetas = columnsMetaFuture.get();
            dataSourceMeta.setTablesMetaList(tableMetas);
            List<DataSourceTableMeta> columnsMeta = keyMetaFuture.get();
            dataSourceMeta.setTablesKeysMetaList(columnsMeta);
            List<DataSourceIndexMeta> dataSourceIndexMetas = indexInfoMetaFuture.get();
            dataSourceMeta.setTablesIndexMetaList(dataSourceIndexMetas);
            CacheUtils.put(key, dataSourceMeta);
            return AjaxResult.success(dataSourceMeta);
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.success(dataSourceMeta);
        }
    }


    @Override
    public AjaxResult showTableSql(String databaseKey, String table) {

        String sql = "show create table " + table;
        Map<String, Object> dataResult = JdbcUtils.findOneResult(databaseKey, sql, null);
        Map<String, String> resultMap = new HashMap<>(2);

        //create 模板
        if (ObjectUtil.notEqual("1", dataResult.get("code"))) {
            return AjaxResult.error(dataResult.get("msg").toString());
        }
        resultMap.put("createTableSql", dataResult.get("Create Table").toString());

        //insert 模板
        String inertSql = TableFieldSqlUtils.getInertSql(databaseKey);
        if (ObjectUtil.isNotNull(inertSql)) {
            String dataBaseName = DataSourceFactory.getDataBaseName(databaseKey);
            String insertSql = StrFormatter.format(inertSql, dataBaseName, table);
            Map<String, Object> dataResult2 = JdbcUtils.findOneResult(databaseKey, insertSql, null);
            if (ObjectUtil.equal("1", dataResult2.get("code"))) {
                resultMap.put("insertSql", dataResult2.get("insertText").toString());
            }
        }
        resultMap.put("updateSql", "todo");
        resultMap.put("deleteSql", "todo");
        return AjaxResult.success(resultMap);
    }


    @Override
    public DataSourceModel selectDbById(Long id) {
        return dbSourceRepository.selectById(id);
    }

    @Override
    public List<DbSqlText> sqlTextListAll() {
        return dbSqlTextRepository.findAll();
    }

    @Override
    public void deleteDataSourceAll() {
        List<DataSourceModel> all = dbSourceRepository.findAll();
        for (DataSourceModel dataSourceModel : all) {
            try {
                deleteDataBaseSource(dataSourceModel.getId());
            } catch (Exception ignored) {
            }
        }
    }
}
