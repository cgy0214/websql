package com.itboy.dao;

import com.itboy.model.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @ClassName : SysUserRoleRepository
 * @Description :
 * @Author : 超
 * @Date: 2023/1/28 23:14
 */
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {

    /**
     * 根据userId 查询所有角色
     *
     * @param loginId
     * @return
     */
    @Query(value = "select  * FROM AUTH_USER_ROLE WHERE USER_ID= ?1", nativeQuery = true)
    List<SysUserRole> findUserRole(Long loginId);

    @Transactional
    @Query("DELETE FROM SysUserRole WHERE userId= ?1")
    @Modifying
    void deleteByUserId(Long userId);
}
