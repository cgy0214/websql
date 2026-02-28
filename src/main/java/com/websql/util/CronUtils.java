package com.websql.util;

import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.pattern.CronPatternUtil;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName : CronUtils
 * @Description : cron工具类
 * @Author : rabbit boy_0214@sina.com
 * @Date: 2026/2/27 15:35
 */
public class CronUtils {

    private static final ConcurrentHashMap<String, CronPattern> PATTERN_CACHE = new ConcurrentHashMap<>(16);

    public static boolean isValid(String cron) {
        try {
            getOrCreatePattern(cron);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Date getNextDate(String cron) {
        try {
            CronPattern pattern = getOrCreatePattern(cron);
            if (pattern == null) {
                return null;
            }
            return CronPatternUtil.nextDateAfter(
                    pattern,
                    new Date(),
                    true
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static CronPattern getOrCreatePattern(String cron) {
        return PATTERN_CACHE.computeIfAbsent(cron, CronPattern::new);
    }

    public static void clearCache() {
        PATTERN_CACHE.clear();
    }
}
