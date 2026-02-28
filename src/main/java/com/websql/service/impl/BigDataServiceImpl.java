package com.websql.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
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
import com.websql.service.strategy.SqlExecutionStrategy;
import com.websql.service.strategy.SqlStrategyFactory;
import com.websql.task.ScheduleUtils;
import com.websql.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.sql.Connection;
import java.sql.SQLException;
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

    @Resource
    private SqlStrategyFactory sqlStrategyFactory;

    @Override
    public Result<BigDataTaskModel> queryTaskList(BigDataTaskModel model) {
        Result<BigDataTaskModel> result = new Result<>();
        PageRequest pageRequest = PageRequest.of(model.getPage() - 1, model.getLimit());
        Specification<BigDataTaskModel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ObjectUtil.isNotNull(model.getId())) {
                predicates.add(cb.equal(root.get("id"), model.getId()));
            }
            if (ObjectUtil.isNotEmpty(model.getTaskName())) {
                predicates.add(cb.like(root.get("taskName"), "%" + model.getTaskName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getTaskType())) {
                predicates.add(cb.equal(root.get("taskType"), model.getTaskType()));
            }
            Long currentTeamId = StpUtils.getCurrentActiveTeam().getId();
            predicates.add(cb.equal(root.get("teamId"), currentTeamId));
            // 排序规则：已发布 > 未发布 > 草稿，然后按更新时间倒排序
            Expression<Object> statusOrder = cb.selectCase()
                    .when(cb.equal(root.get("status"), "已发布"), 1)
                    .when(cb.equal(root.get("status"), "未发布"), 2)
                    .when(cb.equal(root.get("status"), "草稿"), 3)
                    .otherwise(4);
            query.orderBy(cb.asc(statusOrder), cb.desc(root.get("updateTime")));
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
        if (ObjectUtil.isEmpty(model.getTaskName())) {
            throw new RuntimeException("任务名称不能为空!");
        }
        if (ObjectUtil.isNotNull(model.getId())) {
            BigDataTaskModel updateModel = bigDataTaskRepository.findById(model.getId()).orElse(null);
            if (ObjectUtil.isNull(updateModel)) {
                throw new RuntimeException("任务不存在,请刷新再试!");
            }
            long count = bigDataTaskRepository.countByTitle(model.getTaskName(),model.getId(),currentTeamId);
            if (count > 0) {
                throw new RuntimeException("任务名称已存在,请重新输入!");
            }
            updateModel.setSqlContent(model.getSqlContent());
            updateModel.setUpdateTime(currentTime);
            updateModel.setUpdateUser(currentUser);
            updateModel.setTaskName(model.getTaskName());
            updateModel.setCron(model.getCron());
            updateModel.setDescription(model.getDescription());
            bigDataTaskRepository.saveAndFlush(updateModel);
        } else {
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
            if (ObjectUtil.isNotNull(model.getTaskId())) {
                predicates.add(cb.equal(root.get("taskId"), model.getTaskId()));
            }
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
                .sorted(Comparator.comparing(BigDataTaskModel::getId, Comparator.reverseOrder())).map(model -> {
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
    public void deleteInstanceAll() {
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
                    SqlExecutionStrategy strategy = sqlStrategyFactory.getStrategy(vo.getOperationType());
                    strategy.execute(connection, vo, resultItem);
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
}
