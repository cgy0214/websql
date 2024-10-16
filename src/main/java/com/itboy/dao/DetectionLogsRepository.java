package com.itboy.dao;

import com.itboy.model.SysDetectionLogsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionLogsRepository extends JpaSpecificationExecutor<SysDetectionLogsModel>, JpaRepository<SysDetectionLogsModel, Long> {


}
