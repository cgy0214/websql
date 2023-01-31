package com.itboy.util;

import cn.dev33.satoken.secure.SaSecureUtil;
import org.springframework.util.StringUtils;

/**
 * @ClassName PasswordUtil
 * @Description 可自行抽离更改，颜值登录在MyShiroRealm62行。
 * @Author 超 boy_0214@sina.com
 * @Date 2019/7/5 0005 12:05
 **/
public class PasswordUtil {

    public static String createPassword(String password, String salt) {
        if (StringUtils.isEmpty(password) || StringUtils.isEmpty(salt)) {
            throw new NullPointerException("生成密码参数不能为空");
        }
        return SaSecureUtil.md5(password.trim().toLowerCase() + salt);
    }

}
