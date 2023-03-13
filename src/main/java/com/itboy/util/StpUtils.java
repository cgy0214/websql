package com.itboy.util;

import cn.dev33.satoken.exception.ApiDisabledException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.itboy.model.SysUser;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @ClassName : StpUtils
 * @Description : 权限工具类
 * @Author 超 boy_0214@sina.com
 * @Date: 2023/1/28 22:40
 */
public class StpUtils {

    private final static String SESSION_USER_KEY = "login_user";

    public static SysUser getCurrentUser() {
        if (!getCurrentLogin()) {
            throw new RuntimeException("当前用户暂未登录!");
        }
        SysUser user = (SysUser) StpUtil.getSession().get(SESSION_USER_KEY);
        return user;
    }

    public static String getCurrentUserName() {
        return getCurrentUser().getName();
    }

    public static Boolean getCurrentLogin() {
        return StpUtil.isLogin();
    }

    public static Boolean currentSuperAdmin() {
        return Long.valueOf(StpUtil.getLoginId().toString()) == 1L ? true : false;
    }


    public static void currentLogout() {
        StpUtil.logout();
    }

    public static String login(SysUser user) {
        StpUtil.login(user.getUserId());
        StpUtil.getSession().set(SESSION_USER_KEY, user);
        return user.getName();
    }


    /**
     * 检查开放平台权限
     */
    public static void checkOpenAuth() {
        if (!EnvBeanUtil.getBoolean("open-api-enabled")) {
            throw new ApiDisabledException("openApi服务未开启!");
        }
        if (!ObjectUtil.equal(getRequest().getHeader("auth"), EnvBeanUtil.getString("open-api-password"))) {
            throw new ApiDisabledException("openApi密码错误!");
        }
        SysUser sysUser = new SysUser();
        sysUser.setName("openApi");
        sysUser.setUserId(999L);
        sysUser.setUserName("openApi");
        login(sysUser);
    }

    private static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
    }
}
