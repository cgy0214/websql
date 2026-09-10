package com.websql.dao;

import com.websql.model.SysDetectionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DetectionRepository extends JpaSpecificationExecutor<SysDetectionModel>, JpaRepository<SysDetectionModel, Long> {


    @Query(value = "select  * FROM SYS_DETECTION_INFO WHERE ID= ?1", nativeQuery = true)
    SysDetectionModel selectById(Long id);

    /**
     * 给只保存了数据源名称的历史监测任务回填数据源ID
     *
     * @param dataSourceId 数据源ID
     * @param dataBaseName 数据源名称
     * @return 更新条数
     */
    @Transactional
    @Modifying
    @Query("UPDATE SysDetectionModel SET dataSourceId = ?1 WHERE dataSourceId IS NULL AND dataBaseName = ?2")
    int updateDataSourceId(Long dataSourceId, String dataBaseName);
}
