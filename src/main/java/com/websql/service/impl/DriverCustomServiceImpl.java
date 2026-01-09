package com.websql.service.impl;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.websql.dao.SysDriverConfigRepository;
import com.websql.model.AjaxResult;
import com.websql.model.DataSourceModel;
import com.websql.model.Result;
import com.websql.model.SysDriverConfig;
import com.websql.service.DriverCustomService;
import com.websql.util.CacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * ClassName  DriverCustomServiceImpl
 * Description 加载外部JDBC驱动实现类
 * Author rabbit boy_0214@sina.com
 * Date 2026/01/09 16:52
 **/
@Service
@Slf4j
public class DriverCustomServiceImpl implements DriverCustomService {

    private URLClassLoader customClassLoader;

    @Resource
    private SysDriverConfigRepository sysDriverConfigRepository;

    String PATH = "C:\\Users\\Administrator\\Desktop\\test";


    @Override
    public void systemLoadConfigDriver() {
        List<SysDriverConfig> sysDriverConfig = JSON.parseArray(ResourceUtil.readUtf8Str("dataSourceTemplate.json"), SysDriverConfig.class);
        sysDriverConfigRepository.saveAll(sysDriverConfig);
    }

    public void loadDriverCustomAll() {
        SysDriverConfig param = new SysDriverConfig();
        param.setTypeName("自定义");
        List<SysDriverConfig> list = sysDriverConfigRepository.findAll(Example.of(param));
        loadJarsFromDir(PATH);
        int count = 0;
        for (SysDriverConfig sysDriverConfig : list) {
            try {
                Class<?> driverClass = customClassLoader.loadClass(sysDriverConfig.getDriverClass());
                DriverManager.registerDriver((Driver) driverClass.getDeclaredConstructor().newInstance());
                ++count;
            } catch (Exception e) {
                log.error("id:{},名称:{},加载自定义驱动失败:{}", sysDriverConfig.getId(), sysDriverConfig.getName(), e.getMessage(), e);
            }
        }
        log.info("load Custom Driver {} Success.", count);
    }

    /**
     * 加载指定目录中的所有JAR文件
     */
    public void loadJarsFromDir(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + dirPath);
        }

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null || files.length == 0) {
            throw new IllegalStateException("No JAR files found in directory: " + dirPath);
        }
        List<URL> urls = new ArrayList<>();
        for (File file : files) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        customClassLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

    @Override
    public URLClassLoader getCustomClassLoader() {
        return this.customClassLoader;
    }

    @Override
    public Connection getDriverConnection(DataSourceModel model) throws SQLException {
        Connection conn = DriverManager.getConnection(model.getDbUrl().trim(),
                model.getDbAccount().trim(), model.getDbPassword().trim());
        return conn;
    }

    @Override
    public List<Map<String, String>> findDriverConfigListSelect(String id) {
        //根据ID查询单条数据
        if (ObjectUtil.isNotNull(id)) {
            SysDriverConfig sysDriverConfig = CacheUtils.get("driver_config_data" + id, SysDriverConfig.class);
            if (ObjectUtil.isNull(sysDriverConfig)) {
                sysDriverConfig = sysDriverConfigRepository.findById(Long.valueOf(id)).get();
                CacheUtils.put("driver_config_data" + id, sysDriverConfig);
            }
            return Collections.singletonList(JSON.parseObject(JSON.toJSONString(sysDriverConfig), Map.class));
        }
        List<Map<String, String>> driverList = CacheUtils.get("driver_config_list", List.class);
        if (ObjectUtil.isNull(driverList)) {
            driverList = new ArrayList<>(0);
            List<SysDriverConfig> list = sysDriverConfigRepository.findAll();
            for (SysDriverConfig dataSourceModel : list) {
                Map<String, String> item = new HashMap<>(4);
                item.put("code", dataSourceModel.getId().toString());
                item.put("value", dataSourceModel.getName());
                item.put("select", "false");
                driverList.add(item);
            }
            CacheUtils.put("driver_config_list", driverList);
        }
        return driverList;
    }


    @Override
    public Result<SysDriverConfig> selectDriverConfigList(SysDriverConfig model) {
        Result<SysDriverConfig> result = new Result<>();
        Specification<SysDriverConfig> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>(3);
            if (ObjectUtil.isNotEmpty(model.getName())) {
                predicates.add(cb.like(root.get("name"), "%" + model.getName() + "%"));
            }
            if (ObjectUtil.isNotEmpty(model.getTypeName())) {
                predicates.add(cb.equal(root.get("typeName"), model.getTypeName()));
            }
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
        Page<SysDriverConfig> all = sysDriverConfigRepository.findAll(spec, PageRequest.of(model.getPage() - 1, model.getLimit()));
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }


    @Override
    public AjaxResult deleteDriverConfig(Long id) {
        SysDriverConfig sysDriverConfig = sysDriverConfigRepository.selectById(id);
        if (ObjectUtil.isNull(sysDriverConfig)) {
            return AjaxResult.error("没有找到此驱动配置信息，请刷新页面再试!");
        }
        if (ObjectUtil.equal(sysDriverConfig.getTypeName(), "内置")) {
            return AjaxResult.error("内置驱动配置不允许删除!");
        }
        sysDriverConfigRepository.deleteById(id);
        CacheUtils.remove("driver_config_list");
        CacheUtils.remove("driver_config_data" + id);
        return AjaxResult.success("删除成功");
    }


    @Override
    public AjaxResult saveOrUpdateDriverConfig(SysDriverConfig sysDriverConfig) {
        if (ObjectUtil.isNotNull(sysDriverConfig.getId())) {
            CacheUtils.remove("driver_config_data" + sysDriverConfig.getId());
        }
        CacheUtils.remove("driver_config_list");
        if (ObjectUtil.equal(sysDriverConfig.getTypeName(), "内置")) {
            return AjaxResult.error("内置驱动配置不允许修改!");
        }
        loadJarsFromDir(PATH);
        try {
            Class<?> driverClass = customClassLoader.loadClass(sysDriverConfig.getDriverClass());
            DriverManager.registerDriver((Driver) driverClass.getDeclaredConstructor().newInstance());
            sysDriverConfigRepository.save(sysDriverConfig);
        } catch (Exception e) {
            log.error("id:{},名称:{},新增自定义驱动失败:{}", sysDriverConfig.getId(), sysDriverConfig.getName(), e.getMessage(), e);
        }
        return AjaxResult.success("新增成功");
    }


    @Override
    public List<SysDriverConfig> findDriverConfigList(String id) {
        if (ObjectUtil.isNotEmpty(id)) {
            SysDriverConfig sysDriverConfig = sysDriverConfigRepository.findById(Long.valueOf(id)).orElse(new SysDriverConfig());
            return Collections.singletonList(sysDriverConfig);
        }
        return sysDriverConfigRepository.findAll();
    }


}
