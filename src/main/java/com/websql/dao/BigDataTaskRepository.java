package com.websql.dao;

import com.websql.model.BigDataTaskModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BigDataTaskRepository extends JpaSpecificationExecutor<BigDataTaskModel>, JpaRepository<BigDataTaskModel, Long>  {

    @Query("select count(1) from BigDataTaskModel where taskName=?1 and id<>?2 and teamId=?3")
    long countByTitle(String taskName, Long id, Long currentTeamId);
}
