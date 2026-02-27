package com.websql.util;

import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.pattern.CronPatternUtil;

import java.util.Date;

/**
 * @ClassName : CronUtils
 * @Description : cron工具类
 * @Author : rabbit boy_0214@sina.com
 * @Date: 2026/2/27 15:35
 */
public class CronUtils {

    public static boolean isValid(String cron) {
        try {
            new CronPattern(cron);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Date getNextDate(String cron) {
        if (!isValid(cron)) {
            return null;
        }
        try {
            return CronPatternUtil.nextDateAfter(
                    new CronPattern(cron),
                    new Date(),
                    true
            );
        } catch (Exception e) {
            return null;
        }
    }
}
