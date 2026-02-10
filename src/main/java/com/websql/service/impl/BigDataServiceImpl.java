package com.websql.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.meta.MetaUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.websql.config.CalciteDataSourceConfig;
import com.websql.config.CalciteSqlParseHandler;
import com.websql.config.DataSourceFactory;
import com.websql.config.JdbcUtils;
import com.websql.dao.BigDataInstanceRepository;
import com.websql.dao.BigDataTaskRepository;
import com.websql.model.*;
import com.websql.service.BigDataService;
import com.websql.task.ScheduleUtils;
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
        ScheduleUtils.removeBigDataTask(id);
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
        ThreadUtil.execAsync(() -> {
            bigDataInstanceRepository.save(model);
        });
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
    public void updateTaskById(BigDataTaskModel bigDataTaskModel) {
        if(ObjectUtil.isNull(bigDataTaskModel.getId())){
            throw new RuntimeException("任务ID不存在!");
        }
        String currentUser = StpUtils.getCurrentUserName();
        String currentTime = DateUtil.now();
        bigDataTaskModel.setReleaseTime(currentTime);
        bigDataTaskModel.setReleaseUser(currentUser);
        bigDataTaskRepository.saveAndFlush(bigDataTaskModel);
    }

    @Override
    public List<BigDataTaskModel> queryListAll() {
        return bigDataTaskRepository.findAll();
    }

    @Override
    public void deleteTaskAll() {
        bigDataTaskRepository.deleteAll();
        bigDataInstanceRepository.deleteAll();
    }

    @Override
    public List<ExecuteResult> execute(BigDataTaskModel model) {
        List<ExecuteResult> results = new ArrayList<>();
        String fullSql = model.getSqlContent();

        String pattern = "--\\*+--\\s*--[\\s\\S]*?--\\*+--\\s*";
        fullSql = fullSql.replaceAll(pattern, "");

        if (cn.hutool.core.util.StrUtil.isBlank(fullSql)) {
            results.add(new ExecuteResult().error("", "SQL内容为空!"));
            return results;
        }

        List<String> sqlStatements = cn.hutool.core.util.StrUtil.split(fullSql, ';');
        Set<String> allSchemas = new HashSet<>();
        List<ParseResultVo> parsedStatements = new ArrayList<>();
        List<String> executableSqlList = new ArrayList<>();

        for (String sql : sqlStatements) {
            if (cn.hutool.core.util.StrUtil.isBlank(sql)) continue;
            try {
                ParseResultVo vo = CalciteSqlParseHandler.parseSql(sql);
                allSchemas.addAll(vo.getSchemas());
                parsedStatements.add(vo);
                executableSqlList.add(sql);
            } catch (Exception e) {
                results.add(new ExecuteResult().error(sql, "解析失败:" + e.getMessage()));
            }
        }

        if (executableSqlList.isEmpty()) {
            results.add(new ExecuteResult().error(fullSql, "未解析出数据源,请检查数据源标识是否与sql中一致!"));
            return results;
        }

        List<CalciteDataSourceParams> params = new ArrayList<>();
        for (String schemaName : allSchemas) {
            DruidDataSource bigDataSource = DataSourceFactory.getBigDataSource(schemaName);
            if (bigDataSource == null) {
                log.error("{},数据源不存在,请先配置数据源!", schemaName);
                continue;
            }
            try (Connection conn = bigDataSource.getConnection()) {
                String catalog = MetaUtil.getCatalog(conn);
                String schema = MetaUtil.getSchema(conn);
                params.add(new CalciteDataSourceParams(schemaName, bigDataSource, catalog, schema));
            } catch (SQLException e) {
                log.error("获取数据源连接失败", e);
                results.add(new ExecuteResult().error(fullSql, "获取数据源[" + schemaName + "]连接失败: " + e.getMessage()));
            }
        }

        if (params.isEmpty()) {
            results.add(new ExecuteResult().error(fullSql, "未解析出数据源,请检查数据源标识是否与sql中一致!"));
            return results;
        }
        try (Connection connection = calciteDataSourceConfig.createConnection(params)) {
            for (int i = 0; i < parsedStatements.size(); i++) {
                ParseResultVo vo = parsedStatements.get(i);
                String sql = executableSqlList.get(i);
                long startTime = System.currentTimeMillis();

                ExecuteResult resultItem = new ExecuteResult();
                resultItem.setSql(sql);

                try {
                    if (isModificationOperation(vo.getOperationType())) {
                        int count = bigDataExecute(connection, sql);
                        resultItem.setStatus(ExecuteResult.STATUS_SUCCESS);
                        resultItem.setData(count);
                        resultItem.setType(ExecuteResult.TYPE_NON_SELECT);
                    } else if (ObjectUtil.equal(vo.getOperationType(), SqlOperationType.SELECT_INSERT)) {
                        int count = bigDataInsertOverSelect(connection, vo);
                        resultItem.setStatus(ExecuteResult.STATUS_SUCCESS);
                        resultItem.setData(count);
                        resultItem.setType(ExecuteResult.TYPE_NON_SELECT);
                    } else {
                        List<Map<String, Object>> data = bigDataSelect(connection, sql);
                        resultItem.setStatus(ExecuteResult.STATUS_SUCCESS);
                        resultItem.setData(data);
                        resultItem.setType(ExecuteResult.TYPE_SELECT);
                    }
                } catch (Exception e) {
                    resultItem.setStatus(ExecuteResult.STATUS_FAIL);
                    resultItem.setErrorMessage(e.getMessage());
                    resultItem.setType(ExecuteResult.TYPE_NON_SELECT);
                    log.error("SQL执行异常: {}", sql, e);
                } finally {
                    resultItem.setTime(System.currentTimeMillis() - startTime);
                }
                results.add(resultItem);
            }
        } catch (SQLException e) {
            log.error("连接创建或执行致命错误", e);
            results.add(new ExecuteResult().error(fullSql, "SQL执行环境初始化失败: " + e.getMessage()));
        }

        return results;
    }

    /**
     * 查询并插入
     *
     * @param connection 数据连接
     * @param vo         参数
     * @return 插入的记录数
     */
    private int bigDataInsertOverSelect(Connection connection, ParseResultVo vo) {
        try {
            List<Map<String, Object>> data = bigDataSelect(connection, vo.getSelectSql());
            if (data.isEmpty()) {
                log.warn("查询结果为空，无需插入");
                return 0;
            }
            String insertWithParamsSql = vo.getInsertWithParamsSql();
            String cleanInsertSql = insertWithParamsSql.replaceAll("(?i)INSERT INTO\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\.([a-zA-Z_][a-zA-Z0-9_]*)", "INSERT INTO $2");
            String schema = vo.getTargetTableInfo().getSchema();
            int insertCount = JdbcUtils.batchInsert(DataSourceFactory.getBigDataSourceKeyName(schema), cleanInsertSql, data);
            log.debug("批量插入完成，成功插入 {} 条记录", insertCount);
            return insertCount;
        } catch (Exception e) {
            log.error("SQL执行异常: {}", e.getMessage(), e);
            throw new RuntimeException("插入操作失败: " + e.getMessage(), e);
        }
    }

    private boolean isModificationOperation(SqlOperationType type) {
        return type.getCode().equals(SqlOperationType.UPDATE.getCode())
                || type.getCode().equals(SqlOperationType.DELETE.getCode())
                || type.getCode().equals(SqlOperationType.INSERT.getCode());
    }

    /**
     * 执行语句
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
     * 查询语句
     *
     * @param connection
     * @param sqlContent
     * @throws SQLException
     */
    private List<Map<String, Object>> bigDataSelect(Connection connection, String sqlContent) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sqlContent)) {

            java.sql.ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnLabel(i));
            }

            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String columnName : columnNames) {
                    Object value = resultSet.getObject(columnName);
                    row.put(columnName, value);
                }
                resultList.add(row);
            }
        } catch (SQLException e) {
            throw new SQLException(e);
        }
        return resultList;
    }


}
