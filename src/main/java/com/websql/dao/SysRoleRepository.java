package com.websql.dao;

import com.websql.model.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @ClassName SysRoleRepository
 * @Description 角色数据访问接口
 * @Author rabbit boy_0214@sina.com
 * @Date 2024/02/10 10:00
 */
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {


}
