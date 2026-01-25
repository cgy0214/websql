package com.websql.dao;

import com.websql.model.BigDataTaskModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BigDataTaskRepository extends JpaSpecificationExecutor<BigDataTaskModel>, JpaRepository<BigDataTaskModel, Long>  {

}
