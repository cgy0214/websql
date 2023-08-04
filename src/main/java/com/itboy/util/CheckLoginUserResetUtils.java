package com.itboy.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;

/**
 * @ClassName : CheckLoginUserResetUtils
 * @Description : 重置用户密码
 * @Author : 超 boy_0214@sina.com
 * @Date: 2023/3/13 15:35
 */
public class CheckLoginUserResetUtils {


    /**
     * 生成验证码
     *
     * @return
     */
    public static String get() {
        String simpleUUID = IdUtil.simpleUUID();
        CacheUtils.put("unlockLoginUserUUID", simpleUUID);
        return simpleUUID;
    }

    /**
     * 检查是否正常
     *
     * @param code
     * @return
     */
    public static Boolean check(String code) {
        String oldCode = CacheUtils.get("unlockLoginUserUUID", String.class);
        if (ObjectUtil.equal(code, oldCode)) {
            CacheUtils.remove("unlockLoginUserUUID");
            return true;
        }
        return false;
    }
}
