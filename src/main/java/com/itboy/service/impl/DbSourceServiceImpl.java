package com.itboy.service.impl;

import cn.hutool.core.util.ObjectUtil;
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
    public void insertLog(SysLog logs) {
        sysLogRepository.save(logs);
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
                Map<String, String> item = new HashMap<>(2);
                item.put("code", dataSourceModel.getDbName());
                item.put("value", dataSourceModel.getDbName());
                dataSourceList.add(item);
            }
            CacheUtils.put("data_source_model", dataSourceList);
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
