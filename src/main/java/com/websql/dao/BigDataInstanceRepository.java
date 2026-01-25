package com.websql.dao;

import com.websql.model.BigDataInstanceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BigDataInstanceRepository extends JpaSpecificationExecutor<BigDataInstanceModel>, JpaRepository<BigDataInstanceModel, Long> {

}
