package com.itboy.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DbSourceFactory;
import com.itboy.config.JdbcUtils;
import com.itboy.config.SqlDruidParser;
import com.itboy.dao.*;
import com.itboy.model.*;
import com.itboy.service.DbSourceService;
import com.itboy.util.CacheUtils;
import com.itboy.util.StpUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.util.*;
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
    private DbSourceFactory dbSourceFactory;

    @Override
    public Result<DataSourceModel> selectDbSourceList(DataSourceModel result) {
        Result<DataSourceModel> result1 = new Result<DataSourceModel>();
        PageRequest pageRequest = new PageRequest(result.getPage() - 1, result.getLimit());
        Specification<DataSourceModel> spec = (Specification<DataSourceModel>) (root, query, cb) -> {
            Path<String> dbName = root.get("dbName");
            Path<String> dbAccount = root.get("dbAccount");
            Path<String> dbUrl = root.get("dbUrl");
            String dbUrl1 = result.getDbUrl() == null ? "" : result.getDbUrl();
            String account1 = result.getDbAccount() == null ? "" : result.getDbAccount();
            String dbname2 = result.getDbName() == null ? "" : result.getDbName();
            Predicate p1 = cb.like(dbName, "%" + dbname2 + "%");
            Predicate p2 = cb.like(dbAccount, "%" + account1 + "%");
            Predicate p3 = cb.like(dbUrl, "%" + dbUrl1 + "%");
            Predicate p = cb.and(p1, p2, p3);
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
        PageRequest request = new PageRequest(model.getPage() - 1, model.getLimit());
        Specification<DbSqlText> spec = (Specification<DbSqlText>) (root, query, cb) -> {
            Path<String> title = root.get("title");
            Path<String> sqlText = root.get("sqlText");
            String sqlText1 = model.getSqlText() == null ? "" : model.getSqlText();
            String title1 = model.getTitle() == null ? "" : model.getTitle();
            Predicate p2 = cb.like(sqlText, "%" + sqlText1 + "%");
            Predicate p3 = cb.like(title, "%" + title1 + "%");
            Predicate p = cb.and(p2, p3);
            query.orderBy(cb.desc(root.get("id")));
            return p;
        };
        Page<DbSqlText> all = dbSqlTextRepository.findAll(spec, request);
        result1.setList(all.getContent());
        result1.setCount((int) all.getTotalElements());
        return result1;
    }

    @Override
    public DataSourceModel delDbSource(String id) {
        Long ids = Long.valueOf(id);
        CacheUtils.remove("data_source_model");
        Optional<DataSourceModel> model = dbSourceRepository.findById(ids);
        dbSourceRepository.deleteById(ids);
        return model.get();
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
        CacheUtils.remove("sql_text_model");
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
        Page<SysLog> all = sysLogRepository.findAll(spec, new PageRequest(model.getPage() - 1, model.getLimit()));
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

    @Override
    public Map executeSql(ExecuteSql sql) {
        Map result = new HashMap(3);
        result.put("code", 1);
        SysUser user = StpUtils.getCurrentUser();
        String userName = user.getUserId() + ":" + user.getUserName();
        SysLog log = new SysLog().setLogName("sql执行记录").setLogDate(DateUtil.now()).setLogContent(sql.getSqlText()).setLogType("1").setLogType("sql执行记录").setLogDbSource(sql.getDataBaseName()).setUserid(userName);
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
            if (sqlParser.get("executeType").equals("SELECT")) {
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
            SysSetup sysSetup = dbSourceFactory.getSysSetUp();
            if (sysSetup.getCol3() == 1) {
                sysLogRepository.save(log);
            }
        }
        return result;
    }

    @Override
    public Integer selectDbByName(String paramName) {
        return dbSourceRepository.findDataSourceByName(paramName);
    }

    @Override
    public void addDbSource(DataSourceModel model) {
        CacheUtils.remove("data_source_model");
        dbSourceRepository.save(model);
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
                item.put("select", "false");
                dataSourceList.add(item);
            }
            CacheUtils.put("data_source_model", dataSourceList);
        }
        //按最近使用的数据源推荐并选中此数据源
        String source = sysLogRepository.querySysLogDataSource();
        dataSourceList.stream().filter(s -> s.get("code").equals(source)).forEach(s -> s.put("short", "1"));
        dataSourceList.stream().filter(s -> !s.get("code").equals(source)).forEach(s -> s.put("short", "2"));
        dataSourceList.sort(Comparator.comparingInt((Map o) -> Integer.valueOf(o.get("short").toString())));
        if (dataSourceList.size() > 0) {
            dataSourceList.stream().skip(1).forEach(s -> s.put("select", "false"));
            dataSourceList.get(0).put("select", "true");
        }
        return dataSourceList;
    }

    @Override
    public void saveSqlText(DbSqlText model) {
        model.setSqlCreateUser(StpUtils.getCurrentUserName());
        CacheUtils.remove("sql_text_model");
        dbSqlTextRepository.save(model);
    }

    @Override
    public List<Map<String, String>> sqlTextList(DataSourceModel model) {
        List<Map<String, String>> sqlTextModelList = CacheUtils.get("sql_text_model", List.class);
        if (ObjectUtil.isNull(sqlTextModelList)) {
            sqlTextModelList = new ArrayList<>();
            List<DbSqlText> all = dbSqlTextRepository.findAll().stream().sorted(Comparator.comparing(DbSqlText::getId).reversed()).collect(Collectors.toList());
            for (DbSqlText dbSqlText : all) {
                Map<String, String> item = new HashMap<>(2);
                item.put("code", dbSqlText.getSqlText());
                item.put("value", dbSqlText.getTitle());
                sqlTextModelList.add(item);
            }
            CacheUtils.put("sql_text_model", sqlTextModelList);
        }
        return sqlTextModelList;
    }


}
