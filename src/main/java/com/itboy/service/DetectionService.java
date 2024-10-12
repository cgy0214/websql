package com.itboy.service;

import com.itboy.model.Result;
import com.itboy.model.SysDetectionModel;

/**
 * @ClassName : DetectionService
 * @Description : 检测服务
 * @Author : 超 boy_0214@sina.com
 * @Date: 2043/10/12 16:32
 */
public interface DetectionService {

    Result<SysDetectionModel> list(SysDetectionModel model);

    SysDetectionModel add(SysDetectionModel model);

    void deleteById(Long id);

    void updateById(SysDetectionModel vo);
}
