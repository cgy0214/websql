package com.itboy.util;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.hutool.cache.file.LRUFileCache;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;

/**
 * @ClassName PasswordUtil
 * @Description 可自行抽离更改，颜值登录在MyShiroRealm62行。
 * @Author 超 boy_0214@sina.com
 * @Date 2019/7/5 0005 12:05
 **/
public class PasswordUtil {

    private static final LRUFileCache CACHE = new LRUFileCache(1000, 500, 2000);

    /**
     * 兼容低版本区分前缀
     */
    private static final String KEY = ".KEY";

    public static String createPassword(String password, String salt) {
        if (ObjectUtil.isEmpty(password) || ObjectUtil.isEmpty(salt)) {
            throw new NullPointerException("生成密码参数不能为空");
        }
        return SaSecureUtil.md5(password.trim().toLowerCase() + salt);
    }

    /**
     * 加密
     *
     * @param content 原文
     * @return
     */
    public static String encrypt(String content) {
        if (ObjectUtil.isEmpty(content)) {
            return null;
        }
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, aesKey());
        return aes.encryptHex(content) + KEY;
    }

    /**
     * 解密
     *
     * @param content 密文
     * @return
     */
    public static String decrypt(String content) {
        if (ObjectUtil.isEmpty(content)) {
            return null;
        }
        if (!StrUtil.contains(content, KEY)) {
            return content;
        }
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, aesKey());
        String newContent = StrUtil.removeSuffix(content, KEY);
        return aes.decryptStr(newContent, CharsetUtil.CHARSET_UTF_8);
    }


    /**
     * 获取本地key
     *
     * @return
     */
    private static byte[] aesKey() {
        try {
            String path = StrUtil.format("{}{}data{}web_sql_aes.key", System.getProperty("user.dir"), FileUtil.FILE_SEPARATOR, FileUtil.FILE_SEPARATOR);
            if (FileUtil.exist(path)) {
                return CACHE.getFileBytes(path);
            } else {
                byte[] key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
                FileUtil.writeBytes(key, path);
                return key;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
