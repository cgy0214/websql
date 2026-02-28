package com.websql.dao;

import com.websql.model.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @ClassName SysUserRepository
 * @Description 用户数据访问接口
 * @Author rabbit boy_0214@sina.com
 * @Date 2024/02/10 10:00
 */
@Repository
public interface SysUserRepository extends JpaSpecificationExecutor<SysUser>, JpaRepository<SysUser, Long> {
    SysUser findByUserName(String userName);

    @Transactional(rollbackFor = Exception.class)
    @Query("update  SysUser set  state = ?2 WHERE userName= ?1")
    @Modifying
    public void updateStateByUserName(String userName,Integer state);
}
