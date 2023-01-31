package com.itboy.util;

import cn.dev33.satoken.stp.StpUtil;
import com.itboy.model.SysUser;

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


}
