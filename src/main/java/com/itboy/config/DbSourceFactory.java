package com.itboy.config;

import cn.hutool.core.util.ObjectUtil;
import com.itboy.model.DataSourceModel;
import com.itboy.model.Result;
import com.itboy.model.SysSetup;
import com.itboy.service.DbSourceService;
import com.itboy.service.LoginService;
import com.itboy.util.CacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @ClassName DbSourceFactory
 * @Description 初始化系统配置, 数据源
 * @Author 超 boy_0214@sina.com
 * @Date 2019/6/21 0021 16:50
 **/
@Component
@Slf4j
public class DbSourceFactory {


    @Autowired
    private DbSourceService dbSourceService;

    @Autowired
    private LoginService loginService;

    /**
     * 初始化数据源
     */
    @PostConstruct
    private void initDbSource() {
        SysSetup sysSetup = getSysSetUp();
        //系统第一次启动会加载数据
        if (sysSetup.getInitDbsource() == 0) {
            log.info("Initializing System...");
            loginService.initSystem();
        }
        if (sysSetup.getInitDbsource() == 1) {
            log.info("Initializing DbSources...");
            DataSourceModel dbModel = new DataSourceModel();
            dbModel.setDbState("有效");
            Result<DataSourceModel> result = dbSourceService.selectDbSourceList(dbModel);
            List<DataSourceModel> dblist = result.getList();
            DataSourceFactory.initDataSource(dblist);
        }
    }

    /**
     * 初始化系统配置
     *
     * @return
     */
    public SysSetup getSysSetUp() {
        SysSetup sysSetup = CacheUtils.get("sys_setup", SysSetup.class);
        if (ObjectUtil.isNull(sysSetup)) {
            sysSetup = dbSourceService.initSysSetup();
            CacheUtils.putNoDue("sys_setup", sysSetup);
        }
        return sysSetup;
    }
}
