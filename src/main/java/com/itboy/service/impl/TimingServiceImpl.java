package com.itboy.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.SqlDruidParser;
import com.itboy.dao.JobLogsRepository;
import com.itboy.dao.TimingRepository;
import com.itboy.model.JobLogs;
import com.itboy.model.Result;
import com.itboy.model.TimingVo;
import com.itboy.service.TimingService;
import com.itboy.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: websql
 * @description: 作业服务业务处理
 * @author: 超 boy_0214@sina.com
 * @create: 2019-09-16 19:28
 **/
@Service
@Slf4j
public class TimingServiceImpl implements TimingService {

    @Resource
    private TimingRepository timingRepository;
    @Resource
    private JobLogsRepository jobLogsRepository;
    @PersistenceContext
    private EntityManager em;

    @Override
    public TimingVo addtimingData(TimingVo model) throws Exception {
        model.setSqlCreateUser(StpUtils.getCurrentUserName());
        Map<String, Object> sqlParser = SqlDruidParser.sqlParser(model.getTimingName(), model.getSqlText());
        if (sqlParser.get("executeType") == null) {
            throw new RuntimeException("SQL解析异常");
        }
        return timingRepository.save(model);
    }

    @Override
    public void delTiming(String id) {
        Long ids = Long.valueOf(id);
        timingRepository.deleteById(ids);
    }


    @Override
    public Result<TimingVo> timingList(TimingVo model) {
        Result<TimingVo> result = new Result<>();
        Specification<TimingVo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>(4);
            if (ObjectUtil.isNotEmpty(model.getCol2())) {
                predicates.add(cb.like(root.get("col2"), "%" + model.getCol2() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getTimingName())) {
                predicates.add(cb.like(root.get("timingName"), "%" + model.getTimingName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getSyncName())) {
                predicates.add(cb.like(root.get("syncName"), "%" + model.getSyncName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getCol1())) {
                predicates.add(cb.like(root.get("col1"), "%" + model.getCol1() + "%"));
            }
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
        Page<TimingVo> all = timingRepository.findAll(spec, new PageRequest(model.getPage() - 1, model.getLimit()));
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public void updateTiming(TimingVo vo) {
        timingRepository.saveAndFlush(vo);
    }

    @Override
    public void updateStatus(TimingVo vo) {
        timingRepository.saveAndFlush(vo);
    }

    @Override
    public void saveLogs(JobLogs logs) {
        jobLogsRepository.save(logs);
    }

    @Override
    public Result<JobLogs> jobLogsList(JobLogs model) {
        Result<JobLogs> result = new Result<>();
        Specification<JobLogs> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>(3);
            if (ObjectUtil.isNotEmpty(model.getTaskName())) {
                predicates.add(cb.like(root.get("taskName"), "%" + model.getTaskName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getExecuteDate())) {
                predicates.add(cb.like(root.get("executeDate"), "%" + model.getExecuteDate() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getTaskState())) {
                predicates.add(cb.like(root.get("taskState"), "%" + model.getTaskState() + "%"));
            }
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
        Page<JobLogs> all = jobLogsRepository.findAll(spec, new PageRequest(model.getPage() - 1, model.getLimit()));
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public void jobLogDelete() {
        jobLogsRepository.deleteAll();
    }

}
