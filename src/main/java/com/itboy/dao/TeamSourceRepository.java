package com.itboy.dao;

import com.itboy.model.TeamSourceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamSourceRepository extends JpaSpecificationExecutor<TeamSourceModel>, JpaRepository<TeamSourceModel, Long> {

}
