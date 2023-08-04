package com.itboy.controller;

import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DataSourceFactory;
import com.itboy.model.AjaxResult;
import com.itboy.model.DataSourceModel;
import com.itboy.model.ExecuteSql;
import com.itboy.service.DbSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName DataSourceController
 * @Description 操作数据源控制
 * @Author 超 boy_0214@sina.com
 * @Date 2019/6/14 0014 17:03
 **/
@Controller
@RequestMapping("/dataSourceManager")
@Slf4j
public class DataSourceManagerController {

    @Resource
    private DbSourceService dbSourceService;


    @RequestMapping("/page")
    public String dataSourcePage() {
        return "dataSourcePage";
    }


    @RequestMapping("dbSource/sqlController/tablePage")
    @ResponseBody
    public String tablePage() {
        //可自行实现
        return "请通过SQL编写查询表";
    }

    @RequestMapping("/addSourcePage")
    public String addSourcePage() {
        return "addSourcePage";
    }


    @RequestMapping("/dataSourceList")
    @ResponseBody
    public AjaxResult dataSourceList(DataSourceModel model) {
        return AjaxResult.success(dbSourceService.selectDbSourceList(model));
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
        Statement pre = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(model.getDbUrl().trim(),
                    model.getDbAccount().trim(), model.getDbPassword().trim());
            pre = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = pre.executeQuery(model.getDbCheckUrl());
            if (rs.first()) {
                return AjaxResult.success("连接成功!");
            } else {
                return AjaxResult.error("连接失败,返回结果集为空!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return AjaxResult.error("连接失败,error:" + e.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pre != null) {
                    pre.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
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
            Integer count = dbSourceService.selectDbByName(model.getDbName());
            if(count>0){
                return AjaxResult.error("连接名称已经存在,请换一个！");
            }
            DataSourceFactory.saveDataSource(model);
            dbSourceService.addDbSource(model);
            log.info("Successful Add  DbSources ");
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }

    /***
     * 删除数据源
     * @param id 主键ID
     * @return
     */
    @RequestMapping("/deleteDataSource/{id}")
    @ResponseBody
    public AjaxResult deleteDataSource(@PathVariable String id) {
        try {
            DataSourceModel model = dbSourceService.delDbSource(id);
            DataSourceFactory.removeDataSource(model.getDbName());
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
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


    /*暂未实现获取表功能*/
    @RequestMapping("dbSource/getTableList")
    @ResponseBody
    public Map getTableList(ExecuteSql sql) {
        Map result = new HashMap(0);
        return result;
    }


}
