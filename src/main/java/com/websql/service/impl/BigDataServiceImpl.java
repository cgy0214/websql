package com.websql.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.websql.dao.BigDataInstanceRepository;
import com.websql.dao.BigDataTaskRepository;
import com.websql.model.BigDataInstanceModel;
import com.websql.model.BigDataTaskModel;
import com.websql.model.Result;
import com.websql.service.BigDataService;
import com.websql.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class BigDataServiceImpl implements BigDataService {

    @Resource
    private BigDataTaskRepository bigDataTaskRepository;

    @Resource
    private BigDataInstanceRepository bigDataInstanceRepository;

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
    public void saveTask(BigDataTaskModel model) {
        if (ObjectUtil.isEmpty(model.getTaskName())) {
            throw new RuntimeException("任务名称不能为空");
        }
        String currentUser = StpUtils.getCurrentUserName();
        String currentTime = DateUtil.now();
        Long currentTeamId = StpUtils.getCurrentActiveTeam().getId();
        if (ObjectUtil.isEmpty(model.getId())) {
            model.setCreateUser(currentUser);
            model.setCreateTime(currentTime);
            model.setTeamId(currentTeamId);
        }
        model.setUpdateTime(currentTime);
        bigDataTaskRepository.save(model);
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
}
