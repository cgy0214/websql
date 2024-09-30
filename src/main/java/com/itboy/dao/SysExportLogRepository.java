package com.itboy.dao;

import com.itboy.model.SysExportModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SysExportLogRepository extends JpaSpecificationExecutor<SysExportModel>, JpaRepository<SysExportModel, Long> {


}
