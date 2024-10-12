package com.itboy.service.impl;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.SqlParserHandler;
import com.itboy.dao.DetectionRepository;
import com.itboy.model.Result;
import com.itboy.model.SqlParserVo;
import com.itboy.model.SysDetectionModel;
import com.itboy.service.DetectionService;
import com.itboy.util.StpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DetectionServiceImpl implements DetectionService {

    private static final Logger log = LoggerFactory.getLogger(DetectionServiceImpl.class);

    @Autowired
    private DetectionRepository detectionRepository;

    @Override
    public Result<SysDetectionModel> list(SysDetectionModel model) {
        Result<SysDetectionModel> result = new Result<>();
        Long teamId = Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId();
        Specification<SysDetectionModel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>(4);
            if (ObjectUtil.isNotEmpty(model.getName())) {
                predicates.add(cb.like(root.get("name"), "%" + model.getName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getDataBaseName())) {
                predicates.add(cb.like(root.get("dataBaseName"), "%" + model.getDataBaseName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getState())) {
                predicates.add(cb.like(root.get("state"), "%" + model.getState() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getId())) {
                predicates.add(cb.and(root.get("id").in(model.getId())));
            }
            predicates.add(cb.and(root.get("teamId").in(teamId)));
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
        Page<SysDetectionModel> all = detectionRepository.findAll(spec, PageRequest.of(model.getPage() - 1, model.getLimit()));
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public SysDetectionModel add(SysDetectionModel model) {
        model.setCreateTime(DateUtil.date());
        model.setCreateUser(StpUtils.getCurrentUserName());
        model.setTeamId(StpUtils.getCurrentActiveTeam().getId());
        String sql = Base64Decoder.decodeStr(model.getSqlContent());
        List<SqlParserVo> parserVoList = SqlParserHandler.getParserVo(model.getDataBaseName(), sql);
        for (SqlParserVo sqlParserVo : parserVoList) {
            if (ObjectUtil.notEqual(sqlParserVo.getMethodType(), SqlParserHandler.SELECT)) {
                throw new RuntimeException("SQL非查询语句类不允许执行!");
            }
        }
        return detectionRepository.save(model);
    }

    @Override
    public void deleteById(Long id) {
        detectionRepository.deleteById(id);
    }

    @Override
    public void updateById(SysDetectionModel vo) {
        detectionRepository.saveAndFlush(vo);
    }
}
