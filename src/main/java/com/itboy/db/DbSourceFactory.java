package com.itboy.db;

import com.itboy.model.DbSourceModel;
import com.itboy.model.Result;
import com.itboy.model.SysSetup;
import com.itboy.service.DbSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @ClassName DbSourceFactory
 * @Description 初始化系统配置,数据源
 * @Author 超
 * @Date 2019/6/21 0021 16:50
 **/
@Component
@Slf4j
public class DbSourceFactory {


    @Autowired
    private DbSourceService dbSourceService;


    @PostConstruct
    private void initDbSource(){
        SysSetup sysSetup =  getSysSetUp();
        if(sysSetup.getInitDbsource()==1){
            log.info("Initializing DbSources...");
            DbSourceModel dbModel = new DbSourceModel();
            dbModel.setDbstate("有效");
            Result<DbSourceModel> result =  dbSourceService.selectDbSourceList(dbModel);
            List<DbSourceModel> dblist = result.getData();
            DataSourceFactory.initDataSource(dblist);
        }
    }

    @Cacheable(value = "sysSetUp")
    public SysSetup getSysSetUp(){
        SysSetup sysSetup = dbSourceService.initSysSetup();
        return sysSetup;
    }
}
