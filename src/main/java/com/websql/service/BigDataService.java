package com.websql.service;

import com.websql.model.BigDataInstanceModel;
import com.websql.model.BigDataTaskModel;
import com.websql.model.Result;

public interface BigDataService {

    Result<BigDataTaskModel> queryTaskList(BigDataTaskModel model);

    void saveTask(BigDataTaskModel model);

    void deleteTask(Long id);

    BigDataTaskModel getTaskById(Long id);

    Result<BigDataInstanceModel> queryInstanceList(BigDataInstanceModel model);

    void saveInstance(BigDataInstanceModel model);

    void deleteInstance(Long id);
}
