package com.websql.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.meta.MetaUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.websql.config.CalciteDataSourceConfig;
import com.websql.config.CalciteSqlParseHandler;
import com.websql.config.DataSourceFactory;
import com.websql.dao.BigDataInstanceRepository;
import com.websql.dao.BigDataTaskRepository;
import com.websql.model.*;
import com.websql.service.BigDataService;
import com.websql.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BigDataServiceImpl implements BigDataService {

    @Resource
    private BigDataTaskRepository bigDataTaskRepository;

    @Resource
    private BigDataInstanceRepository bigDataInstanceRepository;

    @Resource
    private CalciteDataSourceConfig calciteDataSourceConfig;

    @Override
    public Result<BigDataTaskModel> queryTaskList(BigDataTaskModel model) {
        Result<BigDataTaskModel> result = new Result<>();
        PageRequest pageRequest = PageRequest.of(model.getPage() - 1, model.getLimit());
        Specification<BigDataTaskModel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(model.getTaskName())) {
                predicates.add(cb.like(root.get("taskName"), "%" + model.getTaskName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getTaskType())) {
                predicates.add(cb.equal(root.get("taskType"), model.getTaskType()));
            }
            Long currentTeamId = StpUtils.getCurrentActiveTeam().getId();
            predicates.add(cb.equal(root.get("teamId"), currentTeamId));
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<BigDataTaskModel> all = bigDataTaskRepository.findAll(spec, pageRequest);
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public BigDataTaskModel saveTask(BigDataTaskModel model) {
        String currentUser = StpUtils.getCurrentUserName();
        String currentTime = DateUtil.now();
        Long currentTeamId = StpUtils.getCurrentActiveTeam().getId();
        if (ObjectUtil.isNotNull(model.getId())) {
            BigDataTaskModel updateModel = bigDataTaskRepository.findById(model.getId()).orElse(null);
            if (ObjectUtil.isNull(updateModel)) {
                throw new RuntimeException("任务不存在,请刷新再试!");
            }
            updateModel.setSqlContent(model.getSqlContent());
            updateModel.setUpdateTime(currentTime);
            updateModel.setUpdateUser(currentUser);
            updateModel.setCron(model.getCron());
            updateModel.setDescription(model.getDescription());
            bigDataTaskRepository.saveAndFlush(updateModel);
        } else {
            if (ObjectUtil.isEmpty(model.getTaskName())) {
                throw new RuntimeException("任务名称不能为空!");
            }
            BigDataTaskModel param = new BigDataTaskModel();
            param.setTaskName(model.getTaskName());
            param.setTeamId(currentTeamId);
            long count = bigDataTaskRepository.count(Example.of(param));
            if (count > 0) {
                throw new RuntimeException("任务名称已存在,请重新输入!");
            }
            model.setCreateUser(currentUser);
            model.setCreateTime(currentTime);
            model.setUpdateTime(currentTime);
            model.setUpdateUser(currentUser);
            model.setTeamId(currentTeamId);
            bigDataTaskRepository.save(model);
        }
        return model;
    }

    @Override
    public void deleteTask(Long id) {
        bigDataTaskRepository.deleteById(id);
    }

    @Override
    public BigDataTaskModel getTaskById(Long id) {
        return bigDataTaskRepository.findById(id).orElse(null);
    }

    @Override
    public Result<BigDataInstanceModel> queryInstanceList(BigDataInstanceModel model) {
        Result<BigDataInstanceModel> result = new Result<>();
        PageRequest pageRequest = PageRequest.of(model.getPage() - 1, model.getLimit());
        Specification<BigDataInstanceModel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(model.getTaskName())) {
                predicates.add(cb.like(root.get("taskName"), "%" + model.getTaskName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getInstanceStatus())) {
                predicates.add(cb.equal(root.get("instanceStatus"), model.getInstanceStatus()));
            }
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<BigDataInstanceModel> all = bigDataInstanceRepository.findAll(spec, pageRequest);
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public void saveInstance(BigDataInstanceModel model) {
        String currentUser = StpUtils.getCurrentUserName();
        String currentTime = DateUtil.now();
        if (ObjectUtil.isEmpty(model.getId())) {
            model.setCreateUser(currentUser);
            model.setCreateTime(currentTime);
        }
        bigDataInstanceRepository.save(model);
    }

    @Override
    public void deleteInstance(Long id) {
        bigDataInstanceRepository.deleteById(id);
    }

    @Override
    public List<Map<String, String>> findDataList() {
        return bigDataTaskRepository.findAll().stream()
                .map(model -> {
                    Map<String, String> item = new HashMap<>(2);
                    item.put("code", model.getId().toString());
                    item.put("value", model.getTaskName());
                    item.put("id", model.getId().toString());
                    item.put("select", "false");
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public BigDataTaskModel saveTaskContent(BigDataTaskModel model) {
        BigDataTaskModel updateModel = bigDataTaskRepository.findById(model.getId()).orElse(null);
        if (ObjectUtil.isNull(updateModel)) {
            throw new RuntimeException("任务不存在,请刷新再试!");
        }
        String currentUser = StpUtils.getCurrentUserName();
        String currentTime = DateUtil.now();
        updateModel.setSqlContent(model.getSqlContent());
        updateModel.setUpdateTime(currentTime);
        updateModel.setUpdateUser(currentUser);
        bigDataTaskRepository.saveAndFlush(updateModel);
        return updateModel;
    }

    @Override
    public List execute(BigDataTaskModel model) {
        List<CalciteDataSourceParams> params = new ArrayList<>();
        SqlOperationType operationType;

        try {
            ParseResultVo parseResultVo = CalciteSqlParseHandler.parseSql(model.getSqlContent());
            Set<String> schemas = parseResultVo.getSchemas();
            operationType = parseResultVo.getOperationType();
            if (SqlOperationType.UNKNOWN.equals(operationType)) {
                throw new RuntimeException("请输入正确的sql!");
            }

            for (String schemaName : schemas) {
                DruidDataSource bigDataSource = DataSourceFactory.getBigDataSource(schemaName);
                if (bigDataSource == null) {
                    throw new RuntimeException(schemaName + "数据源不存在,请先配置数据源!");
                }
                String catalog = MetaUtil.getCatalog(bigDataSource.getConnection());
                String schema = MetaUtil.getSchema(bigDataSource.getConnection());
                params.add(new CalciteDataSourceParams(schemaName, bigDataSource, catalog, schema));
            }

        } catch (Exception e) {
            log.error("sql解析失败,", e);
            throw new RuntimeException(e);
        }

        if (params.isEmpty()) {
            throw new RuntimeException("数据源不存在,请先配置数据源!");
        }

        Connection connection = null;
        try {
            connection = calciteDataSourceConfig.createConnection(params);
            if (operationType.getCode().equals(SqlOperationType.UPDATE.getCode())
                    || operationType.getCode().equals(SqlOperationType.DELETE.getCode())
                    || operationType.getCode().equals(SqlOperationType.INSERT.getCode())) {
                int i = bigDataExecute(connection, model.getSqlContent());
                System.out.println("影响行数: " + i);
            } else {
                bigDataSelect(connection, model.getSqlContent());
            }

        } catch (SQLException e) {
            log.error("SQL execution failed: ", e);
        }

        return null;
    }

    /**
     * todo 执行SQL
     *
     * @param connection
     * @param sqlContent
     * @return
     * @throws SQLException
     */
    private int bigDataExecute(Connection connection, String sqlContent) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sqlContent);
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * todo 处理查询
     *
     * @param connection
     * @param sqlContent
     * @throws SQLException
     */
    private void bigDataSelect(Connection connection, String sqlContent) throws SQLException {
        ResultSet resultSet = null;
        try (Statement statement = connection.createStatement()) {
            resultSet = statement.executeQuery(sqlContent);
            while (resultSet.next()) {
                System.out.println(resultSet.getString(1));
                System.out.println(resultSet.getString(2));
                System.out.println(resultSet.getString(3));
                System.out.println("\n");

            }
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }


}
