package com.websql.service.impl;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.websql.config.SqlParserHandler;
import com.websql.dao.DetectionLogsRepository;
import com.websql.dao.DetectionRepository;
import com.websql.model.*;
import com.websql.service.DetectionService;
import com.websql.service.MessageTemplateService;
import com.websql.util.StpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DetectionServiceImpl implements DetectionService {

    private static final Logger log = LoggerFactory.getLogger(DetectionServiceImpl.class);

    @Autowired
    private DetectionRepository detectionRepository;

    @Autowired
    private DetectionLogsRepository detectionLogsRepository;

    @Autowired
    private MessageTemplateService messageTemplateService;

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
        for (SysDetectionModel sysDetectionModel : all.getContent()) {
            if (ObjectUtil.isNotNull(sysDetectionModel.getMessageId())) {
                sysDetectionModel.setMessageName(messageTemplateService.queryMessageTemplateById(sysDetectionModel.getMessageId()).getName());
            }
        }
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
        if (parserVoList.size() != 1) {
            throw new RuntimeException("仅限监测单条查询SQL，请拆分SQL后添加!");
        }
        for (SqlParserVo sqlParserVo : parserVoList) {
            if (ObjectUtil.notEqual(sqlParserVo.getMethodType(), SqlOperationType.SELECT.getCode())) {
                throw new RuntimeException("SQL非查询语句类不允许执行!");
            }
        }
        return detectionRepository.save(model);
    }

    @Override
    public void deleteById(Long id) {
        detectionRepository.deleteById(id);
        deleteLogsByTaskId(id);
    }

    @Override
    public void updateById(SysDetectionModel vo) {
        detectionRepository.saveAndFlush(vo);
    }

    @Override
    public SysDetectionModel selectById(Long id) {
        return detectionRepository.selectById(id);
    }

    @Override
    public List<Map<String, String>> selectAllByActiveId(String id) {
        SysDetectionModel model = new SysDetectionModel();
        model.setTeamId(Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId());
        List<SysDetectionModel> list = detectionRepository.findAll(Example.of(model), Sort.by("id").descending());
        List<Map<String, String>> resultList = new ArrayList<>(list.size());
        for (SysDetectionModel sysDetectionModel : list) {
            Map<String, String> item = new HashMap<>(3);
            item.put("code", sysDetectionModel.getId().toString());
            item.put("value", sysDetectionModel.getName());
            item.put("id", sysDetectionModel.getId().toString());
            item.put("select", ObjectUtil.equal(id, sysDetectionModel.getId().toString()) ? "true" : "false");
            resultList.add(item);
        }
        return resultList;
    }

    @Override
    public Result<SysDetectionLogsModel> logList(SysDetectionLogsModel model) {
        Result<SysDetectionLogsModel> result = new Result<>();
        Long teamId = Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId();
        Specification<SysDetectionLogsModel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>(4);
            if (ObjectUtil.isNotEmpty(model.getName())) {
                predicates.add(cb.like(root.get("name"), "%" + model.getName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getDataBaseName())) {
                predicates.add(cb.like(root.get("dataBaseName"), "%" + model.getDataBaseName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getId())) {
                predicates.add(cb.and(root.get("id").in(model.getId())));
            }
            if (ObjectUtil.isNotEmpty(model.getTaskId())) {
                predicates.add(cb.and(root.get("taskId").in(model.getTaskId())));
            }
            if (ObjectUtil.isNotEmpty(model.getBeginDate())) {
                String beginStr = model.getBeginDate();
                String endStr = model.getEndDate();
                if (!beginStr.contains(":")) {
                    beginStr = beginStr + " 00:00:00";
                    endStr = endStr + " 23:59:59";
                }
                predicates.add(cb.between(root.get("createTime"), DateUtil.parseDateTime(beginStr), DateUtil.parseDateTime(endStr)));
            }
            predicates.add(cb.and(root.get("teamId").in(teamId)));
            query.orderBy(cb.desc(root.get("createTime")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
        Page<SysDetectionLogsModel> all = detectionLogsRepository.findAll(spec, PageRequest.of(model.getPage() - 1, model.getLimit()));
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public Map<String, Object> logCharts(SysDetectionLogsModel model) {
        model.setPage(1);
        model.setLimit(Integer.MAX_VALUE);
        Result<SysDetectionLogsModel> sysDetectionLogsModelResult = logList(model);
        List<SysDetectionLogsModel> list = sysDetectionLogsModelResult.getList();
        
        Map<String, Object> resultMap = new HashMap<>();
        
        Map<String, Integer> statData = new HashMap<>();
        statData.put("total", 0);
        statData.put("normal", 0);
        statData.put("urgent", 0);
        statData.put("alarm", 0);
        statData.put("taskCount", 0);
        int total = list.size();
        long distinctTaskCount = list.stream().map(SysDetectionLogsModel::getTaskId).distinct().count();
        int normal = 0;
        int urgent = 0;
        int alarm = 0;
        
        for (SysDetectionLogsModel log : list) {
            String alarmLevel = log.getAlarmLevel();
            String stateName = log.getStateName();
            if ("紧急".equals(alarmLevel)) {
                urgent++;
            } else {
                normal++;
            }
            if ("推送通知".equals(stateName)) {
                alarm++;
            }
        }
        statData.put("total", total);
        statData.put("normal", normal);
        statData.put("urgent", urgent);
        statData.put("alarm", alarm);
        statData.put("taskCount", (int) distinctTaskCount);
        resultMap.put("statData", statData);
        
        List<String> hourList = new ArrayList<>();
        List<Integer> noAlarmList = new ArrayList<>();
        List<Integer> hasAlarmList = new ArrayList<>();
        
        if (!list.isEmpty()) {
            Map<String, List<SysDetectionLogsModel>> hourGroupMap = list.stream()
                .collect(Collectors.groupingBy(log -> DateUtil.format(log.getCreateTime(), "yyyy-MM-dd HH:00")));
            TreeSet<String> sortedHours = new TreeSet<>(hourGroupMap.keySet());
            
            hourList = new ArrayList<>(sortedHours);
            
            for (String hour : hourList) {
                List<SysDetectionLogsModel> hourLogs = hourGroupMap.get(hour);
                int noAlarm = 0, hasAlarm = 0;
                for (SysDetectionLogsModel log : hourLogs) {
                    String stateName = log.getStateName();
                    if ("推送通知".equals(stateName)) {
                        hasAlarm++;
                    } else {
                        noAlarm++;
                    }
                }
                noAlarmList.add(noAlarm);
                hasAlarmList.add(hasAlarm);
            }
        }
        
        Map<String, Object> trendData = new HashMap<>();
        trendData.put("hourList", hourList);
        trendData.put("noAlarmList", noAlarmList);
        trendData.put("hasAlarmList", hasAlarmList);
        resultMap.put("trendData", trendData);
        
        int normalCount = 0;
        int urgentCount = 0;
        for (SysDetectionLogsModel log : list) {
            String level = log.getAlarmLevel();
            if ("紧急".equals(level)) {
                urgentCount++;
            } else {
                normalCount++;
            }
        }
        
        List<Map<String, Object>> alarmLevelData = new ArrayList<>();
        Map<String, Object> normalItem = new HashMap<>();
        normalItem.put("name", "普通");
        normalItem.put("value", normalCount);
        alarmLevelData.add(normalItem);
        Map<String, Object> urgentItem = new HashMap<>();
        urgentItem.put("name", "紧急");
        urgentItem.put("value", urgentCount);
        alarmLevelData.add(urgentItem);
        resultMap.put("alarmLevelData", alarmLevelData);
        
        List<String> taskNames = new ArrayList<>();
        List<Integer> taskNoAlarmList = new ArrayList<>();
        List<Integer> taskHasAlarmList = new ArrayList<>();
        
        if (!list.isEmpty()) {
            Map<String, List<SysDetectionLogsModel>> taskGroupMap = list.stream()
                .collect(Collectors.groupingBy(SysDetectionLogsModel::getName));
            taskNames = new ArrayList<>(taskGroupMap.keySet());
            
            for (String taskName : taskNames) {
                List<SysDetectionLogsModel> taskLogs = taskGroupMap.get(taskName);
                int noAlarm = 0, hasAlarm = 0;
                for (SysDetectionLogsModel log : taskLogs) {
                    String stateName = log.getStateName();
                    if ("推送通知".equals(stateName)) {
                        hasAlarm++;
                    } else {
                        noAlarm++;
                    }
                }
                taskNoAlarmList.add(noAlarm);
                taskHasAlarmList.add(hasAlarm);
            }
        }
        
        Map<String, Object> taskStatusData = new HashMap<>();
        taskStatusData.put("taskNames", taskNames);
        taskStatusData.put("noAlarmList", taskNoAlarmList);
        taskStatusData.put("hasAlarmList", taskHasAlarmList);
        resultMap.put("taskStatusData", taskStatusData);
        
        Map<String, Integer> datasourceCount = new HashMap<>();
        for (SysDetectionLogsModel log : list) {
            String dbName = log.getDataBaseName();
            if (dbName != null && !dbName.isEmpty()) {
                datasourceCount.put(dbName, datasourceCount.getOrDefault(dbName, 0) + 1);
            }
        }
        List<Map<String, Object>> datasourceData = new ArrayList<>();
        if (datasourceCount.isEmpty()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "无数据");
            item.put("value", 0);
            datasourceData.add(item);
        } else {
            for (Map.Entry<String, Integer> entry : datasourceCount.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("value", entry.getValue());
                datasourceData.add(item);
            }
        }
        resultMap.put("datasourceData", datasourceData);
        
        int[] execTimeRanges = new int[5];
        for (SysDetectionLogsModel log : list) {
            Long execTime = log.getExecTime();
            if (execTime != null) {
                if (execTime < 100) execTimeRanges[0]++;
                else if (execTime < 500) execTimeRanges[1]++;
                else if (execTime < 1000) execTimeRanges[2]++;
                else if (execTime < 5000) execTimeRanges[3]++;
                else execTimeRanges[4]++;
            }
        }
        resultMap.put("execTimeData", execTimeRanges);
        
        return resultMap;
    }

    @Override
    public void deleteLog(Long id) {
        detectionLogsRepository.deleteById(id);
    }

    @Override
    public void deleteLogAll() {
        detectionLogsRepository.deleteAll();
    }

    @Override
    public int countByDataBaseName(String dataBaseName) {
        SysDetectionModel model = new SysDetectionModel();
        model.setTeamId(Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId());
        model.setDataBaseName(dataBaseName);
        return (int) detectionRepository.count(Example.of(model));
    }

    @Override
    public void deleteByDataBaseName(String dataBaseName) {
        SysDetectionModel model = new SysDetectionModel();
        model.setTeamId(Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId());
        model.setDataBaseName(dataBaseName);
        List<SysDetectionModel> detectionModels = detectionRepository.findAll(Example.of(model));
        detectionRepository.deleteAll(detectionModels);
        for (SysDetectionModel detectionModel : detectionModels) {
            deleteLogsByTaskId(detectionModel.getId());
        }
    }

    @Override
    public void deleteLogsByTaskId(Long taskId) {
        SysDetectionLogsModel logModel = new SysDetectionLogsModel();
        logModel.setTeamId(Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId());
        logModel.setTaskId(taskId);
        detectionLogsRepository.deleteAll(detectionLogsRepository.findAll(Example.of(logModel)));
    }
}
