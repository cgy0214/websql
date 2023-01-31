package com.itboy.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.itboy.config.DbSourceFactory;
import com.itboy.config.JdbcUtils;
import com.itboy.config.SqlDruidParser;
import com.itboy.model.*;
import com.itboy.service.DbSourceService;
import com.itboy.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
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
    private DbSourceFactory dbSourceFactory;

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
    public String saveSqlText(DbSqlText model) {
        try {
            dbSourceService.saveSqlText(model);
        } catch (Exception e) {
            return "新增失败！error：" + e.getMessage();
        }
        return "新增成功！";
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

    /***
     * 执行器
     * @param sql
     * @return
     */
    @RequestMapping("/executeSql")
    @ResponseBody
    public Map executeSql(@RequestBody ExecuteSql sql) {
        Map result = new HashMap(3);
        result.put("code", 1);
        SysUser user = StpUtils.getCurrentUser();
        String userName = user.getUserId() + ":" + user.getUserName();
        SysLog log = new SysLog().setLogName("sql执行记录").setLogDate(DateUtil.now()).setLogContent(sql.getSqlText()).setLogType("1").setLogType("sql执行记录").setLogDbSource(sql.getDataBaseName()).setUserid(userName);
        try {
            if (ObjectUtil.isEmpty(sql.getDataBaseName()) || ObjectUtil.isEmpty(sql.getSqlText())) {
                throw new NullPointerException("请选择数据源或编写SQL!");
            }
            Map<String, Object> sqlParser = SqlDruidParser.sqlParser(sql.getDataBaseName(), sql.getSqlText());
            if (sqlParser.get("executeType") == null) {
                throw new NullPointerException("SQL解析异常");
            }
            result.put("sqlExecuteType", sqlParser.get("executeType"));
            List<String> executeSqlList = (List<String>) sqlParser.get("executeSql");
            List dataList = new ArrayList();
            if (sqlParser.get("executeType").equals("SELECT")) {
                for (String executeSql : executeSqlList
                ) {
                    Map<String, Object> resultData = JdbcUtils.findMoreResult(sql.getDataBaseName(), executeSql, new ArrayList<>());
                    dataList.add(resultData);
                }
            } else {
                for (String executeSql : executeSqlList
                ) {
                    Map<String, Object> resultData = JdbcUtils.updateByPreparedStatement(sql.getDataBaseName(), executeSql, new ArrayList<>());
                    dataList.add(resultData);
                }
            }
            log.setLogResult(dataList.toString());
            result.put("dataList", dataList);
        } catch (Exception e) {
            log.setLogResult(e.getMessage());
            e.printStackTrace();
            result.put("code", 2);
            result.put("msg", e.getMessage());
        } finally {
            SysSetup sysSetup = dbSourceFactory.getSysSetUp();
            if (sysSetup.getCol3() == 1) {
                dbSourceService.insertLog(log);
            }
        }
        return result;
    }

}
