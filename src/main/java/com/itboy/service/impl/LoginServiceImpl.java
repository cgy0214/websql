package com.itboy.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DataSourceFactory;
import com.itboy.dao.*;
import com.itboy.model.*;
import com.itboy.service.DbSourceService;
import com.itboy.service.LoginService;
import com.itboy.util.CacheUtils;
import com.itboy.util.PasswordUtil;
import com.itboy.util.StpUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName LoginServiceImpl
 * @Description 登录实现类
 * @Author 超 boy_0214@sina.com
 * @Date 2019/6/26 0026 16:45
 **/
@Service
@Log4j2
public class LoginServiceImpl implements LoginService {


    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private SysUserLogRepository sysUserLogRepository;

    @Autowired
    private SysSetUpRepository sysSetUpRepository;

    @Autowired
    private SysRoleRepository sysRoleRepository;

    @Autowired
    private SysUserRoleRepository sysUserRoleRepository;

    @Resource
    private DbSourceService dbSourceService;

    @Override
    public SysUser findByUserName(String userName) {
        return sysUserRepository.findByUserName(userName);
    }

    @Override
    public AjaxResult login(String userName, String password, String ip) {
        SysUserLog log = new SysUserLog().setUserName(userName).setLogName("系统登录日志").setLogIp(ip).setLoginFlag("登录成功!").setLogDate(DateUtil.now());
        try {
            SysUser user = this.findByUserName(userName);
            if (ObjectUtil.isNull(user)) {
                log.setLoginFlag("账号或密码错误!");
                return AjaxResult.error("账号或密码错误!");
            }
            if (user.getState() != 0) {
                log.setLoginFlag("账号已被禁用!");
                return AjaxResult.error("账号已被禁用!");
            }
            String loginPass = PasswordUtil.createPassword(password, user.getUserName());
            if (!user.getPassword().equals(loginPass)) {
                log.setLoginFlag("账号或密码错误!");
                return AjaxResult.error("账号或密码错误!");
            }
            log.setUserId(user.getUserId());
            return AjaxResult.success(StpUtils.login(user));
        } catch (Exception e) {
            e.printStackTrace();
            log.setLoginFlag(e.getMessage());
            return AjaxResult.error(e.getMessage());
        } finally {
            sysUserLogRepository.save(log);
        }
    }

    @Override
    public Result<SysUserLog> getLogList(SysUserLog model) {
        Result<SysUserLog> result = new Result<>();
        Specification<SysUserLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>(1);
            if (ObjectUtil.isNotEmpty(model.getUserName())) {
                predicates.add(cb.like(root.get("userName"), "%" + model.getUserName() + "%"));
            }
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
        Page<SysUserLog> all = sysUserLogRepository.findAll(spec, new PageRequest(model.getPage() - 1, model.getLimit()));
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public Boolean updateUsers(SysUser sysUser) {
        SysUser sysUserById = sysUserRepository.findById(sysUser.getUserId()).get();
        if (ObjectUtil.isNotEmpty(sysUser.getName())) {
            sysUserById.setName(sysUser.getName());
        }
        if (ObjectUtil.isNotEmpty(sysUser.getEmail())) {
            sysUserById.setEmail(sysUser.getEmail());
        }
        if (ObjectUtil.isNotEmpty(sysUser.getState())) {
            sysUserById.setState(sysUser.getState());
        }
        if (ObjectUtil.isNotEmpty(sysUser.getPassword())) {
            String passNew = PasswordUtil.createPassword(sysUser.getPassword(), sysUserById.getUserName());
            sysUserById.setPassword(passNew);
        }
        sysUserRepository.save(sysUserById);
        return true;
    }

    @Override
    public Boolean updateSysSetUp(SysSetup sys) {
        SysSetup sysSetup = sysSetUpRepository.findById(sys.getId()).get();
        if (ObjectUtil.isNotEmpty(sys.getInitDbsource())) {
            sysSetup.setInitDbsource(sys.getInitDbsource());
        }
        if (ObjectUtil.isNotEmpty(sys.getCol3())) {
            sysSetup.setCol3(sys.getCol3());
        }
        if (ObjectUtil.isNotEmpty(sys.getCol2())) {
            sysSetup.setCol2(sys.getCol2());
        }
        if (ObjectUtil.isNotEmpty(sys.getCol1())) {
            sysSetup.setCol1(sys.getCol1());
        }
        if (ObjectUtil.isNotEmpty(sys.getEnabledHelp())) {
            sysSetup.setEnabledHelp(sys.getEnabledHelp());
        }
        if (ObjectUtil.isNotEmpty(sys.getEnabledLockView())) {
            sysSetup.setEnabledLockView(sys.getEnabledLockView());
        }
        CacheUtils.remove("sys_setup");
        sysSetUpRepository.save(sysSetup);
        return true;
    }


    @Override
    public Result<SysUser> selectUserRoleList(SysUser model) {
        Result<SysUser> result = new Result<>();
        Specification<SysUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>(3);
            if (ObjectUtil.isNotEmpty(model.getName())) {
                predicates.add(cb.like(root.get("name"), "%" + model.getName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getUserName())) {
                predicates.add(cb.like(root.get("userName"), "%" + model.getUserName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getUserId())) {
                predicates.add(cb.equal(root.get("userId"), model.getUserId()));
            }
            query.orderBy(cb.desc(root.get("createTime")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
        Page<SysUser> all = sysUserRepository.findAll(spec, new PageRequest(model.getPage() - 1, model.getLimit()));
        Map<Long, List<SysUserRole>> userRoleMap = sysUserRoleRepository.findAll().stream().collect(Collectors.groupingBy(SysUserRole::getUserId));
        for (SysUser sysUser : all) {
            if (sysUser.getState() == 0) {
                sysUser.setStateName("有效");
            } else {
                sysUser.setStateName("无效");
            }
            List<SysUserRole> sysUserRoles = userRoleMap.get(sysUser.getUserId());
            if (ObjectUtil.isNotNull(sysUserRoles)) {
                sysUser.setSysRoles(sysUserRoles);
                sysUser.setSysRoleName(sysUserRoles.stream().map(SysUserRole::getRole).collect(Collectors.joining(",")));
            }
        }
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public Boolean deleteUserRole(Long id) {
        sysUserRepository.deleteById(id);
        sysUserRoleRepository.delete(new SysUserRole().setUserId(id));
        CacheUtils.remove("user_roles_model");
        return true;
    }

    @Override
    public Boolean updateResetPassword(Long userId, String password) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setPassword(password);
        return updateUsers(sysUser);
    }

    @Override
    public List<SysRole> queryRolesSelect() {
        List<SysRole> resultList = sysRoleRepository.findAll();
        for (SysRole sysRole : resultList) {
            sysRole.setCode(sysRole.getRoleId().toString());
            sysRole.setValue(sysRole.getRole().toString());
        }
        return resultList;
    }

    /**
     * 修改用户信息
     *
     * @param param
     * @return
     */

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateUserRole(SysUser param) {
        LoginServiceImpl loginService = (LoginServiceImpl) AopContext.currentProxy();
        loginService.updateUsers(param);
        loginService.updateUserRoles(param);
        return true;
    }

    /**
     * 保存角色信息
     *
     * @param param
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUserRoles(SysUser param) {
        if (ObjectUtil.isNotEmpty(param.getSysRoleName())) {
            CacheUtils.remove("user_roles_model");
            List<String> roles = Arrays.asList(param.getSysRoleName().split(","));
            sysUserRoleRepository.deleteByUserId(param.getUserId());
            Map<String, SysRole> roleMap = sysRoleRepository.findAll().stream().collect(Collectors.toMap(SysRole::getRole, s -> s, (k1, k2) -> k1));
            for (String role : roles) {
                if (ObjectUtil.isNotEmpty(role)) {
                    SysUserRole sysUserRole = new SysUserRole();
                    sysUserRole.setUserId(param.getUserId());
                    sysUserRole.setRole(role);
                    sysUserRole.setRoleId(roleMap.get(role).getRoleId());
                    sysUserRoleRepository.save(sysUserRole);
                }
            }
        }
        return true;
    }


    /**
     * 新增用户角色信息
     *
     * @param param
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AjaxResult addUserRoleSource(SysUser param) {
        if (ObjectUtil.isNotNull(sysUserRepository.findByUserName(param.getUserName()))) {
            return AjaxResult.error("登录账号已经存在!");
        }
        param.setCreateTime(DateUtil.date());
        String passNew = PasswordUtil.createPassword(param.getPassword(), param.getUserName());
        param.setPassword(passNew);
        sysUserRepository.save(param);
        LoginServiceImpl loginService = (LoginServiceImpl) AopContext.currentProxy();
        loginService.updateUserRoles(param);
        return AjaxResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initSystem() {
        try {
            //初始化超管账号
            SysUser user = new SysUser();
            user.setPassword(PasswordUtil.createPassword("admin", "admin"));
            user.setUserId(1L);
            user.setName("超级管理员");
            user.setUserName("admin");
            user.setState(0);
            user.setCreateTime(DateUtil.date());
            sysUserRepository.save(user);

            //初始化预制角色
            SysRole role1 = new SysRole();
            role1.setRole("super-admin");
            role1.setDescription("超级管理员");
            role1.setRoleId(1L);
            role1.setRemark("至高无上的权利");
            sysRoleRepository.save(role1);

            SysRole role2 = new SysRole();
            role2.setRole("database-admin");
            role2.setDescription("数据源管理角色");
            role2.setRoleId(2L);
            role2.setRemark("所属数据源管理菜单");
            sysRoleRepository.save(role2);

            SysRole role3 = new SysRole();
            role3.setRole("sql-admin");
            role3.setDescription("SQL管理角色");
            role3.setRoleId(3L);
            role3.setRemark("所属SQL管理菜单");
            sysRoleRepository.save(role3);

            SysRole role4 = new SysRole();
            role4.setRole("log-admin");
            role4.setDescription("日志管理角色");
            role4.setRoleId(4L);
            role4.setRemark("所属日志管理菜单");
            sysRoleRepository.save(role4);

            SysRole role5 = new SysRole();
            role5.setRole("timing-admin");
            role5.setDescription("作业管理角色");
            role5.setRoleId(5L);
            role5.setRemark("所属作业管理菜单");
            sysRoleRepository.save(role5);

            //初始化人员角色
            SysUserRole sysUserRole1 = new SysUserRole();
            sysUserRole1.setUserId(user.getUserId());
            sysUserRole1.setRole(role1.getRole());
            sysUserRole1.setRoleId(role1.getRoleId());
            sysUserRoleRepository.save(sysUserRole1);

            //初始化系统设置
            SysSetup sysSetup = SysSetup.getInstance();
            sysSetup.setId(1L);
            sysSetup.setCol1("1");
            sysSetup.setCol2("1");
            sysSetup.setCol3(1);
            sysSetup.setCol4(1);
            sysSetup.setInitDbsource(1);
            sysSetup.setEnabledHelp(0);
            sysSetup.setEnabledLockView(0);
            sysSetUpRepository.save(sysSetup);

            //初始化默认数据源H2
            DataSourceModel model = new DataSourceModel()
                    .setDbName("DEFAULT-H2")
                    .setDriverClass("org.h2.Driver")
                    .setDbUrl("jdbc:h2:./data/database;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=1")
                    .setDbCheckUrl("select 1")
                    .setDbAccount("sa")
                    .setDbPassword("admin@websql")
                    .setInitialSize(1)
                    .setMaxActive(10)
                    .setMaxIdle(5)
                    .setMaxWait(20)
                    .setDbState("有效");
            DataSourceFactory.saveDataSource(model);
            dbSourceService.addDbSource(model);
        } catch (Exception e) {
            log.error("初始化系统报错" + e.getMessage());
            e.printStackTrace();
        }
    }


}
