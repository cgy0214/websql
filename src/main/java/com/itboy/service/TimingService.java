package com.itboy.service;

import com.itboy.model.JobLogs;
import com.itboy.model.Result;
import com.itboy.model.TimingVo;

import java.util.List;

/**
 * @program: websql
 * @description: 作业服务
 * @author: 超 boy_0214@sina.com
 * @create: 2019-09-16 19:28
 **/
public interface TimingService {
    TimingVo addtimingData(TimingVo model) throws Exception;

    void delTiming(Long id);

    Result<TimingVo> timingList(TimingVo model);

    void updateTiming(TimingVo vo);

    void updateStatus(TimingVo vo);

    void saveLogs(JobLogs logs);

    Result<JobLogs> jobLogsList(JobLogs model);

    void jobLogDelete();

    List<TimingVo> queryTimingJobList(String name);

    TimingVo queryTimingJobById(Long id);


}
