package com.websql.service;

import com.websql.model.BigDataInstanceModel;
import com.websql.model.BigDataTaskModel;
import com.websql.model.ExecuteResult;
import com.websql.model.Result;

import java.util.List;
import java.util.Map;

public interface BigDataService {

    Result<BigDataTaskModel> queryTaskList(BigDataTaskModel model);

    BigDataTaskModel saveTask(BigDataTaskModel model);

    void deleteTask(Long id);

    BigDataTaskModel getTaskById(Long id);

    Result<BigDataInstanceModel> queryInstanceList(BigDataInstanceModel model);

    void saveInstance(BigDataInstanceModel model);

    void deleteInstance(Long id);

    List<Map<String, String>> findDataList();

    BigDataTaskModel saveTaskContent(BigDataTaskModel model);

    /**
     * 更新任务发布状态
     * @param bigDataTaskModel 任务模型
     */
    void updateTaskById(BigDataTaskModel bigDataTaskModel);

    /**
     * 执行大数据SQL
     * @param model 任务模型
     * @return 执行结果列表
     */
    List<ExecuteResult> execute(BigDataTaskModel model);

    /**
     * 查询所有任务
     * @return 任务列表
     */
    List<BigDataTaskModel> queryListAll();

    /**
     * 删除所有任务
     */
    void deleteTaskAll();

    /**
     * 删除所有实例
     */
    void deleteInstanceAll();

    /**
     * 获取任务创建趋势数据
     */
    Map<String, Object> getTaskTrend(String startDate, String endDate, String taskId);

    /**
     * 获取实例创建趋势数据
     */
    Map<String, Object> getInstanceTrend(String startDate, String endDate, String groupBy, String taskId);

    /**
     * 获取实例状态统计数据
     */
    Map<String, Object> getInstanceStatusStats(String startDate, String endDate, String taskId);

    /**
     * 获取任务执行时间分布数据
     */
    Map<String, Object> getTaskTimeDist(String startDate, String endDate, String taskId);

}
