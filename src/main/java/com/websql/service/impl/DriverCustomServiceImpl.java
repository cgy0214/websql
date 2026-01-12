package com.websql.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.websql.dao.SysDriverConfigRepository;
import com.websql.model.*;
import com.websql.service.DriverCustomService;
import com.websql.util.CacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    @Value("${export.driver.path}")
    private String driverPath;


    @Override
    public void systemLoadConfigDriver() {
        List<SysDriverConfig> sysDriverConfig = JSON.parseArray(ResourceUtil.readUtf8Str("dataSourceTemplate.json"), SysDriverConfig.class);
        sysDriverConfigRepository.saveAll(sysDriverConfig);
    }

    public void loadDriverCustomAll() {
        SysDriverConfig param = new SysDriverConfig();
        param.setTypeName("自定义");
        List<SysDriverConfig> list = sysDriverConfigRepository.findAll(Example.of(param));
        if (list.isEmpty()) {
            return;
        }
        loadJarsFromDir();
        if (ObjectUtil.isNull(customClassLoader)) {
            log.warn("load custom driver error,No Driver Jar ClassLoader");
            return;
        }
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
        log.info("load custom Driver {} Success", count);
    }

    /**
     * 加载指定目录中的所有JAR文件
     */
    public void loadJarsFromDir() {
        String dirPath = driverPath;
        if (ObjectUtil.isEmpty(dirPath)) {
            dirPath = System.getProperty("user.dir") + FileUtil.FILE_SEPARATOR + "data" + FileUtil.FILE_SEPARATOR + "driverLibs";
        }
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
            log.error("load custom Driver error,,No in directory " + dirPath);
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null || files.length == 0) {
            log.error("load custom Driver error,No JAR files found in directory: " + dirPath);
            return;
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
        try {
            if (ObjectUtil.equal(model.getDriverTypeName(), "内置")) {
                return DriverManager.getConnection(model.getDbUrl().trim(),
                        model.getDbAccount().trim(), model.getDbPassword().trim());
            }
            Class<?> driverClass = customClassLoader.loadClass(model.getDriverClass());
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            if (driver.acceptsURL(model.getDbUrl().trim())) {
                return driver.connect(model.getDbUrl().trim(),
                        new java.util.Properties() {{
                            put("user", model.getDbAccount().trim());
                            put("password", model.getDbPassword().trim());
                        }});
            }
        } catch (Exception e) {
            throw new SQLException("Failed to create connection", e);
        }
        throw new SQLException("No suitable driver found for " + model.getDbUrl());
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
        loadJarsFromDir();
        try {
            if (ObjectUtil.isNull(customClassLoader)) {
                return AjaxResult.error("新增失败,没有找到对应驱动,请检查驱动目录是否存在或查看帮助文档!");
            }
            Class<?> driverClass = customClassLoader.loadClass(sysDriverConfig.getDriverClass());
            DriverManager.registerDriver((Driver) driverClass.getDeclaredConstructor().newInstance());
            sysDriverConfigRepository.save(sysDriverConfig);
            return AjaxResult.success("新增成功");
        } catch (Exception e) {
            log.error("id:{},名称:{},新增自定义驱动失败:{}", sysDriverConfig.getId(), sysDriverConfig.getName(), e.getMessage(), e);
            return AjaxResult.error("新增失败," + e.getMessage());
        }
    }


    @Override
    public List<SysDriverConfig> findDriverConfigList(String id) {
        if (ObjectUtil.isNotEmpty(id)) {
            SysDriverConfig sysDriverConfig = sysDriverConfigRepository.findById(Long.valueOf(id)).orElse(new SysDriverConfig());
            return Collections.singletonList(sysDriverConfig);
        }
        return sysDriverConfigRepository.findAll();
    }

    @Override
    public String downloadDriver(DriverDependencyQo driverDependencyQo) {
        if (ObjectUtil.isNotNull(driverDependencyQo.getType()) && ObjectUtil.equal(driverDependencyQo.getType(), "maven")) {
            try {
                DriverDependencyQo xmlModel = parseDependency(driverDependencyQo.getXmlContent());
                driverDependencyQo.setArtifactId(xmlModel.getArtifactId());
                driverDependencyQo.setGroupId(xmlModel.getGroupId());
                driverDependencyQo.setVersion(xmlModel.getVersion());
                driverDependencyQo.setCentral("https://repo1.maven.org/maven2");
            } catch (Exception e) {
                log.error("解析maven坐标失败:{}", e.getMessage(), e);
                throw new RuntimeException("解析坐标失败:" + e.getMessage());
            }
        } else {
            if (ObjectUtil.isEmpty(driverDependencyQo.getCentral())) {
                throw new RuntimeException("下载地址不能为空!");
            }
        }
        try {
            String pathLibs = driverPath;
            if (ObjectUtil.isEmpty(pathLibs)) {
                pathLibs = System.getProperty("user.dir") + FileUtil.FILE_SEPARATOR + "data" + FileUtil.FILE_SEPARATOR + "driverLibs";
            }
            return downloadJar(driverDependencyQo, pathLibs);
        } catch (IOException e) {
            log.error("下载驱动失败:{}", e.getMessage(), e);
            throw new RuntimeException("下载驱动失败:" + e.getMessage());
        }
    }

    private static String downloadJar(DriverDependencyQo dependencyQo, String targetDir) throws IOException {
        String downloadUrl = dependencyQo.getCentral();
        String targetPath = targetDir;
        if (ObjectUtil.isNotNull(dependencyQo.getType()) && ObjectUtil.equal(dependencyQo.getType(), "maven")) {
            String groupPath = dependencyQo.getGroupId().replace('.', '/');
            String jarName = dependencyQo.getArtifactId() + "-" + dependencyQo.getVersion() + ".jar";
            String relativePath = String.format("%s/%s/%s/%s", groupPath, dependencyQo.getArtifactId(), dependencyQo.getVersion(), jarName);
            downloadUrl = downloadUrl + "/" + relativePath;
            targetPath = Paths.get(targetDir, jarName).toString();
        }
        log.info("下载驱动开始: {},\n 保存路径:{}", downloadUrl, targetPath);
        Files.createDirectories(Paths.get(targetDir));
        try (BufferedInputStream in = new BufferedInputStream(new URL(downloadUrl).openStream());
             FileOutputStream out = new FileOutputStream(targetPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        log.info("下载驱动完成: {}", targetPath);
        return targetPath;
    }


    private static DriverDependencyQo parseDependency(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new org.xml.sax.InputSource(
                new java.io.StringReader(xmlContent)));
        Element root = document.getDocumentElement();
        String groupId = getElementText(root, "groupId");
        String artifactId = getElementText(root, "artifactId");
        String version = getElementText(root, "version");
        return new DriverDependencyQo(groupId, artifactId, version);
    }

    private static String getElementText(Element parent, String tagName) {
        return parent.getElementsByTagName(tagName).item(0).getTextContent();
    }

}
