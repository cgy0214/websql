package com.websql.service;

import com.websql.model.Result;
import com.websql.model.SysDetectionLogsModel;
import com.websql.model.SysDetectionModel;

import java.util.List;
import java.util.Map;

/**
 * @ClassName : DetectionService
 * @Description : 检测服务
 * @Author : rabbit boy_0214@sina.com
 * @Date: 2043/10/12 16:32
 */
public interface DetectionService {

    Result<SysDetectionModel> list(SysDetectionModel model);

    SysDetectionModel add(SysDetectionModel model);

    void deleteById(Long id);

    void updateById(SysDetectionModel vo);

    SysDetectionModel selectById(Long id);

    List<Map<String, String>> selectAllByActiveId(String activeId);

    Result<SysDetectionLogsModel> logList(SysDetectionLogsModel model);

    Map<String, Object> logCharts(SysDetectionLogsModel model);

    void deleteLog(Long id);

    void deleteLogAll();

    /**
     * 根据数据源名称统计检测任务数量
     * @param dataBaseName 数据源名称
     * @return 检测任务数量
     */
    int countByDataBaseName(String dataBaseName);

    /**
     * 根据数据源名称删除检测任务
     * @param dataBaseName 数据源名称
     */
    void deleteByDataBaseName(String dataBaseName);

    /**
     * 根据任务ID删除关联的日志记录
     * @param taskId 任务ID
     */
    void deleteLogsByTaskId(Long taskId);

    /**
     * 给只保存了数据源名称的历史检测任务回填数据源ID
     * @param dataBaseName 数据源名称
     * @param dataSourceId 数据源ID
     */
    void fillDataSourceId(String dataBaseName, Long dataSourceId);

    /**
     * 解析检测任务执行时使用的数据源名称，优先使用任务保存的数据源ID，历史数据没有ID时降级使用数据源名称
     * @param model 检测任务
     * @return 数据源名称
     */
    String resolveExecuteDataSourceName(SysDetectionModel model);
}
