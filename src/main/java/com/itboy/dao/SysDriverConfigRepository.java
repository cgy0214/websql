package com.itboy.dao;

import com.itboy.model.SysDriverConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SysDriverConfigRepository extends JpaSpecificationExecutor<SysDriverConfig>, JpaRepository<SysDriverConfig, Long> {

}
