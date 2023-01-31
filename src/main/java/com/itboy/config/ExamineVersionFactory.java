package com.itboy.config;

import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.itboy.model.VersionModel;
import com.itboy.util.ScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @ClassName : ExamineVersionFactory
 * @Description : 版本检查
 * @Author 超 boy_0214@sina.com
 * @Date: 2023/1/28 13:12
 */
@Slf4j
@Component
public class ExamineVersionFactory {

    @Value("${examine.version.enabled}")
    private Boolean enabled;

    private VersionModel versionModel;


    @PostConstruct
    public void run() {
        versionModel = JSON.parseObject(ResourceUtil.readUtf8Str("version.json"), VersionModel.class);
        if (!enabled) {
            return;
        }
        ScheduleUtils.Job job = new ScheduleUtils.Job();
        job.setCron("0 0 10 * * ?");
        job.setJobName("examineVersion");
        job.setClassName("com.itboy.config.ExamineVersionFactory");
        job.setMethodName("execute");
        job.setStatus(1);
        try {
            ScheduleUtils.add(job);
            execute("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute(String name) {
        try {
            VersionModel remoteVersion = JSON.parseObject(HttpUtil.get("https://gitee.com/boy_0214/websql/raw/master/src/main/resources/version.json", CharsetUtil.CHARSET_UTF_8), VersionModel.class);
            log.info("远程最新版本:{},发布日期:{}", remoteVersion.getVersion(), remoteVersion.getDate());
            int compare = VersionComparator.INSTANCE.compare(remoteVersion.getVersion(), versionModel.getVersion());
            if (compare > 0) {
                versionModel = remoteVersion;
                versionModel.setPush(true);
            }
        } catch (Exception e) {
            log.error("获取远程版本失败,将不会推送新版本!");
        }
    }

    public VersionModel getVersionModel() {
        return versionModel;
    }
}
