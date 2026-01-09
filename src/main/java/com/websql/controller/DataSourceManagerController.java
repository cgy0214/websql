package com.websql.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.DbType;
import com.websql.config.DataSourceFactory;
import com.websql.config.JdbcUtils;
import com.websql.model.AjaxResult;
import com.websql.model.DataSourceModel;
import com.websql.model.Result;
import com.websql.model.SysDriverConfig;
import com.websql.service.*;
import com.websql.util.PasswordUtil;
import com.websql.util.SpringContextHolder;
import com.websql.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName DataSourceController
 * @Description 操作数据源控制
 * @Author rabbit boy_0214@sina.com
 * @Date 2019/6/14 0014 17:03
 **/
@Controller
@RequestMapping("/dataSourceManager")
@Slf4j
public class DataSourceManagerController {

    @Resource
    private DbSourceService dbSourceService;

    @Resource
    private LoginService loginService;

    @Resource
    private DetectionService detectionService;

    @Resource
    private TimingService timingService;

    @Resource
    private DriverCustomService driverCustomService;


    @RequestMapping("/page")
    public String dataSourcePage() {
        return "dataSourcePage";
    }

    @RequestMapping("/addSourcePage")
    public ModelAndView addSourcePage(@RequestParam(required = false) String id) {
        ModelAndView modelAndView = new ModelAndView("addSourcePage");
        SysDriverConfig sysDriverConfig = new SysDriverConfig();
        modelAndView.addObject("id", id);
        if (ObjectUtil.equal("-1", id)) {
            sysDriverConfig.setName("Custom");
            sysDriverConfig.setCapacity("自定义数据源，需在【系统管理-驱动管理中】添加驱动配置。");
        }
        if (ObjectUtil.isNotEmpty(id) && ObjectUtil.notEqual("-1", id)) {
            List<SysDriverConfig> driverConfigList = driverCustomService.findDriverConfigList(id);
            if (!driverConfigList.isEmpty()) {
                sysDriverConfig = driverConfigList.get(0);
            }
        }
        modelAndView.addObject("data", sysDriverConfig);
        return modelAndView;
    }

    @RequestMapping("/dataSourceChoice")
    public String dataSourceChoice() {
        return "dataSourceChoice";
    }

    @RequestMapping("/dataSourceList")
    @ResponseBody
    public AjaxResult dataSourceList(DataSourceModel model) {
        Result<DataSourceModel> dataSourceModelResult = dbSourceService.selectDbSourceList(model);
        if (StpUtil.hasRole("demo-admin")) {
            dataSourceModelResult.getList().forEach(s -> s.setDbUrl("****************"));
        }
        return AjaxResult.success(dataSourceModelResult);
    }

    /***
     * 测试数据源
     * @param model
     * @return
     */
    @RequestMapping("/checkUrl")
    @ResponseBody
    public AjaxResult checkUrl(@RequestBody DataSourceModel model) {
        Connection conn = null;
        PreparedStatement pre = null;
        ResultSet rs = null;
        try {
            DriverCustomService driverCustomService = SpringContextHolder.getBean(DriverCustomService.class);
             conn = driverCustomService.getDriverConnection(model);
            //todo 兼容自定义数据源
            //conn = DriverManager.getConnection(model.getDbUrl().trim(),
            //        model.getDbAccount().trim(), model.getDbPassword().trim());
            DbType jdbcType = DataSourceFactory.getDbTypeByJdbcUrl(model.getDbUrl().trim(), model.getDriverClass());
            //odps特殊，提交实例运行
            if (jdbcType.equals(DbType.odps)) {
                if (ObjectUtil.isEmpty(conn.getMetaData().getDatabaseProductName())) {
                    return AjaxResult.error("连接失败,获取odps描述为空!");
                }
                return AjaxResult.success("连接成功!");
            }
            pre = conn.prepareStatement(model.getDbCheckUrl());
            rs = pre.executeQuery();
            if (rs.next()) {
                return AjaxResult.success("连接成功!");
            } else {
                return AjaxResult.error("连接失败,返回结果集为空!");
            }
        } catch (Exception e) {
            if (e.getMessage().contains("No suitable driver")) {
                return AjaxResult.error("连接失败,请检查【系统-驱动管理】是否已导入驱动! <br>" + e.getMessage());
            }
            if (e.getMessage().contains("The driver has not received any packets from the server")) {
                return AjaxResult.error("连接失败,请检查与数据库网络是否联通! \n<br>" + e.getMessage());
            }
            log.error("检查链接失败,{}", e.getMessage(), e);
            return AjaxResult.error("连接失败,错误信息:" + e.getMessage());
        } finally {
            JdbcUtils.releaseConn(rs, pre, conn);
        }
    }

    /***
     * 新增数据源
     * @param model
     * @return
     */
    @RequestMapping("/addDataSource")
    @ResponseBody
    public AjaxResult addDataSource(@RequestBody DataSourceModel model) {
        try {
            if (ObjectUtil.isEmpty(model.getDbName())) {
                return AjaxResult.error("连接名称不能为空！");
            }
            if (ObjectUtil.isEmpty(model.getDriverClass())) {
                return AjaxResult.error("驱动名称不能为空！");
            }
            if (ObjectUtil.isEmpty(model.getDbUrl())) {
                return AjaxResult.error("连接地址不能为空！");
            }
            dbSourceService.addDbSource(model, null);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("新增数据源失败,{}", e.getMessage(), e);
            if (e.getMessage().contains("No suitable driver")) {
                return AjaxResult.error("连接失败,请检查【系统-驱动管理】是否已导入驱动! <br>" + e.getMessage());
            }
            if (e.getMessage().contains("The driver has not received any packets from the server")) {
                return AjaxResult.error("连接失败,请检查与数据库网络是否联通! <br>" + e.getMessage());
            }
            log.error("检查链接失败,{}", e.getMessage(), e);
            return AjaxResult.error("连接失败,错误信息:" + e.getMessage());
        }
    }

    /***
     * 删除数据源
     * @param id 主键ID
     * @return
     */
    @RequestMapping("/deleteDataSource/{id}")
    @ResponseBody
    public AjaxResult deleteDataSource(@PathVariable Long id, @RequestParam(required = false, defaultValue = "0") Integer etlCount) {
        try {
            DataSourceModel dataSourceModel = dbSourceService.selectDbById(id);
            dbSourceService.deleteDataBaseSource(id);
            String operator = StpUtils.getCurrentUserName();
            String dbName = dataSourceModel.getDbName();
            log.info("数据源删除操作: 时间={}, 操作人={}, 数据源名称={}, 关联ETL任务数={}",
                    DateUtil.now(), operator, dbName, etlCount);
            return AjaxResult.success("删除成功!");
        } catch (Exception e) {
            log.error("删除失败:{}", e.getMessage(), e);
            return AjaxResult.error("删除失败:" + e.getMessage());
        }
    }

    /***
     * 加载数据源
     * @param model
     * @return
     */
    @RequestMapping("/findDataSourceList")
    @ResponseBody
    public Map findDataSourceList(DataSourceModel model) {
        Map result = new HashMap(2);
        result.put("code", 0);
        result.put("data", dbSourceService.dbsourceSqlList(model));
        return result;
    }

    @RequestMapping("/findDriverConfigList")
    @ResponseBody
    public AjaxResult findDriverConfigList(@RequestParam(required = false) String id) {
        return AjaxResult.success(driverCustomService.findDriverConfigList(id));
    }

    @RequestMapping("/updateDataSourceName")
    @ResponseBody
    public AjaxResult updateDataSourceName(@RequestParam Long id, @RequestParam String name) {
        try {
            if (ObjectUtil.isEmpty(name)) {
                return AjaxResult.error("连接名称不能为空！");
            }
            if (ObjectUtil.isEmpty(id)) {
                return AjaxResult.error("必填参数为空！");
            }
            Integer count = dbSourceService.selectDbByName(name);
            if (count > 0) {
                return AjaxResult.error("连接名称已经存在,请换一个！");
            }
            dbSourceService.updateDataSourceName(id, name);
            log.info("Successful update  DbSources ");
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("修改数据源失败,{}", e.getMessage(), e);
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/showDataSourcePassword")
    @ResponseBody
    public AjaxResult showDataSourcePassword(Long id) {
        if (!StpUtils.currentSuperAdmin()) {
            return AjaxResult.error("抱歉，您不是超级管理员无法查看!");
        }
        DataSourceModel dataSourceModelResult = dbSourceService.selectDbById(id);
        if (ObjectUtil.isNull(dataSourceModelResult)) {
            return AjaxResult.error("请重新刷新页面,没有找到此数据源配置信息!");
        }
        if (ObjectUtil.isNotEmpty(dataSourceModelResult.getDbPassword())) {
            String encrypt = PasswordUtil.decrypt(dataSourceModelResult.getDbPassword());
            dataSourceModelResult.setDbPassword(encrypt);
        }
        if (ObjectUtil.isNotEmpty(dataSourceModelResult.getDbAccount())) {
            String encrypt = PasswordUtil.decrypt(dataSourceModelResult.getDbAccount());
            dataSourceModelResult.setDbAccount(encrypt);
        }
        return AjaxResult.success(dataSourceModelResult);
    }

    @RequestMapping("/countDetectionTaskByDataSourceCode")
    @ResponseBody
    public AjaxResult countDetectionTaskByDataSourceCode(String dataSourceCode) {
        try {
            return AjaxResult.success(detectionService.countByDataBaseName(dataSourceCode));
        } catch (Exception e) {
            log.error("统计检测任务失败,{}", e.getMessage(), e);
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/countEtlTaskByDataSourceCode")
    @ResponseBody
    public AjaxResult countEtlTaskByDataSourceCode(String dataSourceCode) {
        try {
            return AjaxResult.success(timingService.countByDataSourceName(dataSourceCode));
        } catch (Exception e) {
            log.error("统计ETL任务失败,{}", e.getMessage(), e);
            return AjaxResult.error(e.getMessage());
        }
    }

}
