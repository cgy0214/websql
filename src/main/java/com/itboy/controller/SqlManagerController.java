package com.itboy.controller;

import cn.hutool.core.util.ObjectUtil;
import com.itboy.model.AjaxResult;
import com.itboy.model.DataSourceModel;
import com.itboy.model.DbSqlText;
import com.itboy.model.ExecuteSql;
import com.itboy.service.DbSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName : SqlManagerController
 * @Description : SQL管理控制器
 * @Author 超 boy_0214@sina.com
 * @Date: 2023/1/28 23:53
 */
@Controller
@RequestMapping("/sqlManager")
@Slf4j
public class SqlManagerController {


    @Resource
    private DbSourceService dbSourceService;

    @RequestMapping("/sqlPage")
    public String sqlPage() {
        return "sqlPage";
    }

    @RequestMapping("/sqlTextPage")
    public String sqlTextPage() {
        return "sqlTextPage";
    }


    /**
     * 查询sql文本下拉框
     *
     * @param model
     * @return
     */
    @RequestMapping("/querySqlTextSelect")
    @ResponseBody
    public Map sqlTextList(DataSourceModel model) {
        List<Map<String, String>> resultData = dbSourceService.sqlTextList(model);
        Map result = new HashMap(2);
        result.put("code", 0);
        result.put("data", resultData);
        return result;
    }

    /**
     * sql文本列表
     *
     * @param model
     * @return
     */
    @RequestMapping("/querySqlTextList")
    @ResponseBody
    public AjaxResult getSqlTextList(DbSqlText model) {
        return AjaxResult.success(dbSourceService.getDbSqlText(model));
    }


    /**
     * 保存sql文本
     *
     * @param model
     * @return
     */
    @RequestMapping("/saveSqlText")
    @ResponseBody
    public AjaxResult saveSqlText(@RequestBody DbSqlText model) {
        try {
            dbSourceService.saveSqlText(model);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success();
    }


    /**
     * 删除sql
     *
     * @param id
     * @return
     */
    @RequestMapping("/deleteSqlText/{id}")
    @ResponseBody
    public AjaxResult deleteSqlText(@PathVariable String id) {
        try {
            dbSourceService.deleteSqlText(id);
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequestMapping("/sqlTextDeleteAll")
    @ResponseBody
    public AjaxResult sqlTextDeleteAll() {
        try {
            dbSourceService.sqlTextDeleteAll();
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }

    /***
     * 执行器
     * @param sql
     * @return
     */
    @RequestMapping("/executeSql")
    @ResponseBody
    public Map executeSql(@RequestBody ExecuteSql sql) {
        return dbSourceService.executeSql(sql);
    }

    @RequestMapping("/executeSqlNew")
    @ResponseBody
    public AjaxResult executeSqlNew(@RequestBody ExecuteSql sql) {
        if (ObjectUtil.isEmpty(sql.getDataBaseName()) || ObjectUtil.isEmpty(sql.getSqlText())) {
            return AjaxResult.error("请选择数据源或编写SQL!");
        }
        return dbSourceService.executeSqlNew(sql);
    }

    /***
     * 查询数据库所有表及字段
     * @param database 数据源名称
     * @return
     */
    @RequestMapping("/findTableField")
    @ResponseBody
    public AjaxResult findTableField(@RequestParam String database) {
        return dbSourceService.findTableField(database);
    }


    /***
     * 查询数据库表元数据
     * @param database 数据源名称
     * @param table 表名
     * @return
     */
    @RequestMapping("/findMetaTable")
    @ResponseBody
    public AjaxResult findMetaTable(@RequestParam String database, @RequestParam String table) {
        if (ObjectUtil.isEmpty(database) || ObjectUtil.isEmpty(table)) {
            return AjaxResult.error("必填参数为空!");
        }
        return dbSourceService.findMetaTable(database, table);
    }


    /**
     * 建表语句
     *
     * @param database 数据源名称
     * @param table    表名
     * @return
     */
    @RequestMapping("/showTableSql")
    @ResponseBody
    public AjaxResult showTableSql(@RequestParam String database, @RequestParam String table) {
        if (ObjectUtil.isEmpty(database) || ObjectUtil.isEmpty(table)) {
            return AjaxResult.error("必填参数为空!");
        }
        return dbSourceService.showTableSql(database, table);
    }
}
