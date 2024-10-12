package com.itboy.dao;

import com.itboy.model.SysDetectionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionRepository extends JpaSpecificationExecutor<SysDetectionModel>, JpaRepository<SysDetectionModel, Long> {


}
