package com.itboy.service.impl;

import com.itboy.dao.SysSetUpRepository;
import com.itboy.dao.SysUserLogRepository;
import com.itboy.dao.SysUserRepository;
import com.itboy.model.*;
import com.itboy.security.MyShiroRealm;
import com.itboy.service.LoginService;
import com.itboy.util.IpUtil;
import com.itboy.util.PasswordUitl;
import lombok.extern.log4j.Log4j2;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;

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

    @Override
    public SysUser findByUserName(String userName) {
        return sysUserRepository.findByUserName(userName);
    }

    @Override
    public LoginResult login(String userName, String password, HttpServletRequest request) {
        LoginResult loginResult = new LoginResult();
        SysUserLog log = new SysUserLog();
        if(userName==null || userName.isEmpty())
        {
            loginResult.setLogin(false);
            loginResult.setResult("用户名为空");
            return loginResult;
        }
        String msg="登录成功！";
        Subject currentUser = SecurityUtils.getSubject();
        if (currentUser.isAuthenticated() == true) {
            loginResult.setLogin(true);
        }
        UsernamePasswordToken token = new UsernamePasswordToken(userName, password);
        try {
            currentUser.login(token);
            Session session = currentUser.getSession();
            session.setAttribute("userName", userName);
            loginResult.setLogin(true);
            return loginResult;
        }catch (UnknownAccountException e)
        {
            msg = "账号不存在!";
        }
        catch (IncorrectCredentialsException e)
        {
            msg = "账号或密码不正确!";
        }
        catch (AuthenticationException e) {
            msg="用户验证失败";
        }finally {
            log.setLoginFlag(msg);
            log.setUserName(userName);
            log.setLogName("系统登录日志");
            log.setLogIp(IpUtil.getIpAddress(request));
            sysUserLogRepository.save(log);
        }
        loginResult.setLogin(false);
        loginResult.setResult(msg);
        return loginResult;
    }

    @Override
    public void logout() {
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
    }

    @Override
    public Result<SysUserLog> getLogList(SysUserLog model) {
        Result<SysUserLog> result1 = new Result<>();
        Sort sort = new Sort(Sort.Direction.DESC, "id");
        PageRequest request = new PageRequest(model.getPage() - 1, model.getLimit(),sort);
        Specification<SysUserLog> spec = (Specification<SysUserLog>) (root, query, cb) -> {
            Path<String> logName = root.get("logName");
            Path<String> userName = root.get("userName");
            Path<String> loginFlag = root.get("loginFlag");
            Path<String> logIp = root.get("logIp");
            String logName1 =  model.getLogName()==null? "":model.getLogName();
            String userName1 =  model.getUserName()==null? "":model.getUserName();
            String loginFlag1 =  model.getLoginFlag()==null? "":model.getLoginFlag();
            String logIp1 =  model.getLogIp()==null? "":model.getLogIp();
            Predicate p2 =cb.like(logName, "%"+logName1+"%");
            Predicate p3 =cb.like(userName, "%"+userName1+"%");
            Predicate p4 =cb.like(loginFlag, "%"+loginFlag1+"%");
            Predicate p1 =cb.like(logIp, "%"+logIp1+"%");
            Predicate p =  cb.and(p1,p2,p3,p4);
            return p;
        };
        Page<SysUserLog> all = sysUserLogRepository.findAll(spec,request);
        result1.setData(all.getContent());
        result1.setCount((int) all.getTotalElements());
        return result1;
    }

    @Override
    public void updateUsers(SysUser sysUser) {
        SysUser sysUserById  = sysUserRepository.findById(sysUser.getUserId()).get();
        if(!"".equals(sysUser.getName()) && null!=sysUser.getName())sysUserById.setName(sysUser.getName());
        if(!"".equals(sysUser.getEmail()) && null!=sysUser.getEmail())sysUserById.setEmail(sysUser.getEmail());
        if(!"".equals(sysUser.getPassword()) &&null!=sysUser.getPassword()){
           String passNew =  PasswordUitl.queryPassword(sysUser.getPassword(),sysUser.getUserName());
            sysUserById.setPassword(passNew);
        }
        sysUserRepository.save(sysUserById);
        RealmSecurityManager rsm = (RealmSecurityManager)SecurityUtils.getSecurityManager();
        MyShiroRealm realm = (MyShiroRealm)rsm.getRealms().iterator().next();
        realm.clearAllCache();
    }

    @Override
    public void updateSysSetUp(SysSetup sys) {
        SysSetup sysSetup = sysSetUpRepository.findById(sys.getId()).get();
        if(!StringUtils.isEmpty(sys.getInitDbsource()))sysSetup.setInitDbsource(sys.getInitDbsource());
        if(!StringUtils.isEmpty(sys.getCol3()))sysSetup.setCol3(sys.getCol3());
        if(!StringUtils.isEmpty(sys.getCol2()))sysSetup.setCol2(sys.getCol2());
        if(!StringUtils.isEmpty(sys.getCol1()))sysSetup.setCol1(sys.getCol1());
        sysSetUpRepository.save(sysSetup);
    }
}
