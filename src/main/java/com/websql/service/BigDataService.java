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

}
