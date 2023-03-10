package com.itboy.dao;

import com.itboy.model.DataSourceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DbSourceRepository extends JpaSpecificationExecutor<DataSourceModel>, JpaRepository<DataSourceModel, Long> {


}
