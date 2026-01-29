package com.websql.service;

import com.websql.model.BigDataInstanceModel;
import com.websql.model.BigDataTaskModel;
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
}
