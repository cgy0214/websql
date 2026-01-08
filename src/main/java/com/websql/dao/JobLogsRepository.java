package com.websql.dao;

import com.websql.model.JobLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface JobLogsRepository extends JpaSpecificationExecutor<JobLogs>, JpaRepository<JobLogs, Long> {


    @Transactional
    @Query("DELETE FROM JobLogs WHERE taskId= ?1")
    @Modifying
    void deleteByTaskId(Long id);
}
