package com.websql.service.impl;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.websql.model.*;
import com.websql.service.BackupService;
import com.websql.service.BigDataService;
import com.websql.service.DbSourceService;
import com.websql.service.TeamSourceService;
import com.websql.task.ExamineVersionFactory;
import com.websql.util.PasswordUtil;
import com.websql.util.StpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class BackupServiceImpl implements BackupService {


    @Autowired
    private ExamineVersionFactory examineVersionFactory;

    @Autowired
    private TeamSourceService teamSourceService;

    @Autowired
    private DbSourceService dbSourceService;

    @Autowired
    private BigDataService bigDataService;

    public ResponseEntity dataBackups() {
        String currentDir = System.getProperty("user.dir");
        Path backupDir = Paths.get(currentDir, "data", "temp");
        try {
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
        } catch (Exception e) {
            log.error("创建备份目录失败: {}", e.getMessage());
        }

        List<Path> backupFiles = new ArrayList<>();
        String localVersion = examineVersionFactory.getVersionModel().getLocalVersion();
        Map<String, String> metaData = new HashMap<>();
        metaData.put("version", localVersion);
        metaData.put("backupTime", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));

        try {
            // 备份数据源信息
            List<Map<String, Object>> dataSourceJson = downloadDataSourceJson();
            if (!dataSourceJson.isEmpty()) {
                Path dataSourceFile = backupDir.resolve("WEBSQL_" + BackupType.DATA_SOURCE.name() + ".json");
                metaData.put("data", JSONUtil.toJsonStr(dataSourceJson));
                metaData.put("type", BackupType.DATA_SOURCE.name());
                Files.write(dataSourceFile, JSONUtil.toJsonStr(metaData).getBytes(StandardCharsets.UTF_8));
                backupFiles.add(dataSourceFile);
                log.info("数据源备份文件生成成功: {}", dataSourceFile.getFileName());
            }
        } catch (Exception e) {
            log.error("备份数据源生成失败,{}", e.getMessage(), e);
        }

        try {
            // 备份SQL文本
            List<Map<String, Object>> sqlTextJson = downloadSqlTextJson();
            if (!sqlTextJson.isEmpty()) {
                Path sqlTextFile = backupDir.resolve("WEBSQL_" + BackupType.SQL_TEXT.name() + ".json");
                metaData.put("data", JSONUtil.toJsonStr(sqlTextJson));
                metaData.put("type", BackupType.SQL_TEXT.name());
                Files.write(sqlTextFile, JSONUtil.toJsonStr(metaData).getBytes(StandardCharsets.UTF_8));
                backupFiles.add(sqlTextFile);
                log.info("SQL文本备份文件生成成功: {}", sqlTextFile.getFileName());
            }
        } catch (Exception e) {
            log.error("备份SQL列表生成失败,{}", e.getMessage(), e);
        }

        try {
            // 备份团队信息
            List<Map<String, Object>> teamJson = downloadTeamJson();
            if (!teamJson.isEmpty()) {
                Path teamFile = backupDir.resolve("WEBSQL_" + BackupType.TEAM.name() + ".json");
                metaData.put("data", JSONUtil.toJsonStr(teamJson));
                metaData.put("type", BackupType.TEAM.name());
                Files.write(teamFile, JSONUtil.toJsonStr(metaData).getBytes(StandardCharsets.UTF_8));
                backupFiles.add(teamFile);
                log.info("团队信息备份文件生成成功: {}", teamFile.getFileName());
            }
        } catch (Exception e) {
            log.error("备份团队信息生成失败,{}", e.getMessage(), e);
        }

        try {
            // 备份大数据任务
            List<Map<String, Object>> bigDataJson = downloadBigDataJson();
            if (!bigDataJson.isEmpty()) {
                Path bigDataFile = backupDir.resolve("WEBSQL_" + BackupType.BIG_DATA.name() + ".json");
                metaData.put("data", JSONUtil.toJsonStr(bigDataJson));
                metaData.put("type", BackupType.BIG_DATA.name());
                Files.write(bigDataFile, JSONUtil.toJsonStr(metaData).getBytes(StandardCharsets.UTF_8));
                backupFiles.add(bigDataFile);
                log.info("大数据任务备份文件生成成功: {}", bigDataFile.getFileName());
            }
        } catch (Exception e) {
            log.error("备份大数据任务生成失败,{}", e.getMessage(), e);
        }

        if (backupFiles.isEmpty()) {
            try {
                Files.deleteIfExists(backupDir);
            } catch (IOException e) {
                log.warn("清理临时目录失败: {}", e.getMessage());
            }
            return ResponseEntity.badRequest()
                    .body("没有可备份的数据");
        }
        String zipFileName = "websql_backup_" + System.currentTimeMillis() + ".zip";
        Path zipFile = null;
        try {
            zipFile = Files.createTempFile("websql_backup_zip_", ".zip");
            ZipUtil.zip(backupDir.toString(), zipFile.toString());
            log.info("ZIP备份文件创建成功: {}", zipFile.getFileName());

            byte[] zipBytes = Files.readAllBytes(zipFile);
            ByteArrayResource resource = new ByteArrayResource(zipBytes);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
            cleanupTempFiles(backupDir, zipFile);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipBytes.length)
                    .body(resource);
        } catch (Exception e) {
            log.error("读取ZIP文件失败: {}", e.getMessage(), e);
            cleanupTempFiles(backupDir, zipFile);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("读取备份文件失败: " + e.getMessage());
        }
    }

    /**
     * 下载数据源
     */
    public List<Map<String, Object>> downloadDataSourceJson() {
        List<DataSourceModel> dataSourceModels = dbSourceService.reloadDataSourceList();
        if (dataSourceModels.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = dataSourceModels.stream().map(DataSourceModel::getId).collect(Collectors.toList());
        List<TeamResourceModel> resourceModels = teamSourceService.queryTeamResourceById(ids, "DATASOURCE");
        Map<Long, Long> teamMap = resourceModels.stream().collect(Collectors.toMap(TeamResourceModel::getResourceId, TeamResourceModel::getTeamId));
        List<Map<String, Object>> resultList = new ArrayList<>(dataSourceModels.size());
        for (DataSourceModel dataSourceModel : dataSourceModels) {
            String userName = PasswordUtil.decrypt(dataSourceModel.getDbAccount());
            String password = PasswordUtil.decrypt(dataSourceModel.getDbPassword());
            Map<String, Object> item = new HashMap<>(10);
            item.put("title", Base64Encoder.encode(dataSourceModel.getDbName()));
            item.put("url", Base64Encoder.encode(dataSourceModel.getDbUrl()));
            item.put("userName", Base64Encoder.encode(userName));
            item.put("password", Base64Encoder.encode(password));
            item.put("checkSql", dataSourceModel.getDbCheckUrl());
            item.put("driver", dataSourceModel.getDriverClass());
            item.put("initialSize", dataSourceModel.getInitialSize());
            item.put("maxActive", dataSourceModel.getMaxActive());
            item.put("maxIdle", dataSourceModel.getMaxIdle());
            item.put("maxWait", dataSourceModel.getMaxWait());
            item.put("teamId", teamMap.get(dataSourceModel.getId()));
            item.put("druidFilterType", dataSourceModel.getDruidFilterType());
            item.put("sourceIdentifier", dataSourceModel.getSourceIdentifier());
            resultList.add(item);
        }
        return resultList;
    }

    /**
     * 下载sql文本
     */
    public List<Map<String, Object>> downloadSqlTextJson() {
        List<DbSqlText> dataSourceModels = dbSourceService.sqlTextListAll();
        if (dataSourceModels.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> resultList = new ArrayList<>(dataSourceModels.size());
        for (DbSqlText sqlText : dataSourceModels) {
            Map<String, Object> item = new HashMap<>(10);
            item.put("title", Base64Encoder.encode(sqlText.getTitle()));
            item.put("content", Base64Encoder.encode(sqlText.getSqlText()));
            item.put("teamId", sqlText.getTeamId());
            resultList.add(item);
        }
        return resultList;
    }

    /**
     * 下载团队信息
     */
    public List<Map<String, Object>> downloadTeamJson() {
        List<TeamSourceModel> teamSourceModelList = teamSourceService.selectTeamListAll();
        if (teamSourceModelList.isEmpty()) {
            return new ArrayList<>();
        }
        if (teamSourceModelList.size() == 1) {
            TeamSourceModel teamSourceModel = teamSourceModelList.get(0);
            if (teamSourceModel.getId() == 1 && "Default".equals(teamSourceModel.getTeamName())) {
                log.info("只有一个默认团队,不需要导出备份!");
                return new ArrayList<>();
            }
        }
        List<Map<String, Object>> resultList = new ArrayList<>(teamSourceModelList.size());
        for (TeamSourceModel team : teamSourceModelList) {
            if (team.getId() == 1 && "Default".equals(team.getTeamName())) {
                continue;
            }
            Map<String, Object> item = new HashMap<>(10);
            item.put("userId", team.getUserId());
            item.put("teamName", Base64Encoder.encode(team.getTeamName()));
            item.put("description", Base64Encoder.encode(team.getDescription()));
            item.put("state", team.getState());
            item.put("id", team.getId());
            resultList.add(item);
        }
        return resultList;
    }

    /**
     * 下载枢易方舟
     */
    public List<Map<String, Object>> downloadBigDataJson() {
        List<BigDataTaskModel> bigDataTaskModels = bigDataService.queryListAll();
        if (bigDataTaskModels.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> resultList = new ArrayList<>(bigDataTaskModels.size());
        for (BigDataTaskModel bigDataTaskModel : bigDataTaskModels) {
            Map<String, Object> item = new HashMap<>(10);
            item.put("taskName", bigDataTaskModel.getTaskName());
            item.put("teamType", bigDataTaskModel.getTaskType());
            item.put("description", Base64Encoder.encode(bigDataTaskModel.getDescription()));
            item.put("sqlContent", Base64Encoder.encode(bigDataTaskModel.getSqlContent()));
            item.put("cron", bigDataTaskModel.getCron());
            item.put("status", bigDataTaskModel.getStatus());
            item.put("teamId", bigDataTaskModel.getTeamId());
            resultList.add(item);
        }
        return resultList;
    }


    public AjaxResult uploadBackups(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }

        Path tempDir = null;
        StringBuilder message = new StringBuilder();
        AtomicBoolean hasError = new AtomicBoolean(false);

        try {
            String currentDir = System.getProperty("user.dir");
            tempDir = Paths.get(currentDir, "data", "temp", "export");
            Files.createDirectories(tempDir);

            Path zipFile = tempDir.resolve(file.getOriginalFilename());
            file.transferTo(zipFile);
            log.info("保存上传文件: {}", zipFile.getFileName());

            Path extractDir = Files.createTempDirectory(tempDir, "extract_");
            ZipUtil.unzip(zipFile.toString(), extractDir.toString());
            log.info("解压文件完成");

            Files.deleteIfExists(zipFile);

            try (Stream<Path> fileStream = Files.walk(extractDir)) {
                fileStream.filter(Files::isRegularFile)
                        .forEach(filePath -> {
                            String fileName = filePath.getFileName().toString();
                            try {
                                byte[] fileBytes = Files.readAllBytes(filePath);
                                String fileContent = new String(fileBytes, StandardCharsets.UTF_8);

                                if (!validateJsonAndVersion(fileContent, fileName)) {
                                    String errorMsg = "[" + fileName + "]版本验证失败或JSON格式错误";
                                    message.append(errorMsg).append("<br>");
                                    log.error(errorMsg);
                                    hasError.set(true);
                                    return;
                                }

                                ImportResult importResult = importJsonFileWithDetail(fileName, fileContent);
                                if (importResult.isSuccess()) {
                                    message.append("[").append(fileName).append("]导入成功<br>");
                                    log.info("[{}]导入成功", fileName);
                                } else {
                                    message.append("[").append(fileName).append("]导入失败: ").append(importResult.getErrorMessage()).append("<br>");
                                    log.error("[{}]导入失败: {}", fileName, importResult.getErrorMessage());
                                    hasError.set(true);
                                }
                            } catch (Exception e) {
                                String errorMsg = "[" + fileName + "]处理异常: " + e.getMessage();
                                message.append(errorMsg).append("<br>");
                                log.error("处理文件 {} 失败", fileName, e);
                                hasError.set(true);
                            }
                        });
            }

            if (hasError.get()) {
                return AjaxResult.error("备份导入完成，但存在错误", message.toString());
            } else {
                AjaxResult result = AjaxResult.success("备份导入成功");
                result.setDetailMsg(message.toString());
                return result;
            }
        } catch (Exception e) {
            String errorMsg = "上传备份文件处理失败: " + e.getMessage();
            log.error(errorMsg, e);
            message.append(errorMsg);
            return AjaxResult.error(errorMsg, message.toString());
        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * 上传sql文本数据（返回详细结果）
     *
     * @param file JSON数据内容
     * @return 导入结果
     */
    public ImportResult uploadSqlTextJsonWithDetail(String file) {
        try {
            JSONArray dataSourceJson = JSONUtil.parseArray(new String(file.getBytes(), StandardCharsets.UTF_8));
            if (dataSourceJson.isEmpty()) {
                return ImportResult.failure("没有解析出sql文本json数据，请检查！");
            }

            StringBuilder errorMessage = new StringBuilder();
            boolean hasError = false;
            int successCount = 0;

            for (int i = 0; i < dataSourceJson.size(); i++) {
                try {
                    Object object = dataSourceJson.get(i);
                    Map<String, Object> map = (Map<String, Object>) object;
                    DbSqlText dbSqlText = new DbSqlText();
                    dbSqlText.setSqlCreateDate(DateUtil.now());
                    dbSqlText.setSqlText(Base64Decoder.decodeStr(MapUtil.getStr(map, "content")));
                    dbSqlText.setTitle(Base64Decoder.decodeStr(MapUtil.getStr(map, "title")));

                    Long teamId = MapUtil.getLong(map, "teamId");
                    List<TeamSourceModel> teamSourceModels = teamSourceService.queryTeamByIds(Arrays.asList(teamId));
                    //导入的历史团队id不存在，将赋值给当前选中的团队。
                    if (!teamSourceModels.isEmpty()) {
                        dbSqlText.setTeamId(teamId);
                    }
                    dbSourceService.saveSqlText(dbSqlText);
                    successCount++;
                } catch (Exception e) {
                    hasError = true;
                    String title = "未知SQL";
                    try {
                        Map<String, Object> map = (Map<String, Object>) dataSourceJson.get(i);
                        title = Base64Decoder.decodeStr(MapUtil.getStr(map, "title"));
                    } catch (Exception ex) {
                    }
                    errorMessage.append("第").append(i + 1).append("条数据[").append(title)
                            .append("]保存失败: ").append(e.getMessage()).append("; ");
                }
            }

            log.info("导入sql文本数据{}条数据，成功{}条，失败{}条", dataSourceJson.size(), successCount,
                    dataSourceJson.size() - successCount);

            if (hasError) {
                return ImportResult.failure("部分SQL文本导入失败: " + errorMessage.toString());
            }
            return ImportResult.success();
        } catch (Exception e) {
            log.error("导入sql文本数据失败: {}", e.getMessage(), e);
            return ImportResult.failure("导入失败: " + e.getMessage());
        }
    }

    /**
     * 上传数据源信息（返回详细结果）
     *
     * @param file JSON数据内容
     * @return 导入结果
     */
    public ImportResult uploadDataSourceJsonWithDetail(String file) {
        try {
            JSONArray dataSourceJson = JSONUtil.parseArray(file);
            if (dataSourceJson.isEmpty()) {
                return ImportResult.failure("没有解析出json数据，请检查！");
            }

            StringBuilder errorMessage = new StringBuilder();
            boolean hasError = false;
            int successCount = 0;

            for (int i = 0; i < dataSourceJson.size(); i++) {
                try {
                    Object object = dataSourceJson.get(i);
                    Map<String, Object> map = (Map<String, Object>) object;
                    DataSourceModel model = new DataSourceModel();
                    model.setDbName(Base64Decoder.decodeStr(MapUtil.getStr(map, "title")));
                    model.setDbUrl(Base64Decoder.decodeStr(MapUtil.getStr(map, "url")));
                    model.setDbAccount(Base64Decoder.decodeStr(MapUtil.getStr(map, "userName")));
                    model.setDbPassword(Base64Decoder.decodeStr(MapUtil.getStr(map, "password")));
                    model.setDbCheckUrl(MapUtil.getStr(map, "checkSql"));
                    model.setDriverClass(MapUtil.getStr(map, "driver"));
                    model.setInitialSize(MapUtil.getInt(map, "initialSize"));
                    model.setMaxActive(MapUtil.getInt(map, "maxActive"));
                    model.setMaxIdle(MapUtil.getInt(map, "maxIdle"));
                    model.setMaxWait(MapUtil.getInt(map, "maxWait"));
                    model.setSourceIdentifier(MapUtil.getStr(map, "sourceIdentifier"));
                    model.setDruidFilterType(MapUtil.getStr(map, "druidFilterType"));
                    model.setDbPort(MapUtil.getStr(map, "port"));
                    model.setDbState("有效");
                    Long teamId = MapUtil.getLong(map, "teamId");
                    List<TeamSourceModel> teamSourceModels = teamSourceService.queryTeamByIds(Collections.singletonList(teamId));
                    if (teamSourceModels.isEmpty()) {
                        teamId = Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId();
                    }
                    dbSourceService.addDbSource(model, teamId);
                    successCount++;
                } catch (Exception e) {
                    hasError = true;
                    String dbName = "未知数据库";
                    try {
                        Map<String, Object> map = (Map<String, Object>) dataSourceJson.get(i);
                        dbName = Base64Decoder.decodeStr(MapUtil.getStr(map, "title"));
                    } catch (Exception ex) {
                        // 忽略获取名称的异常
                    }
                    errorMessage.append("第").append(i + 1).append("条数据[").append(dbName)
                            .append("]上传失败: ").append(e.getMessage()).append("; ");
                }
            }

            log.info("导入数据源{}条数据，成功{}条，失败{}条", dataSourceJson.size(), successCount,
                    dataSourceJson.size() - successCount);

            if (hasError) {
                return ImportResult.failure("部分数据导入失败: " + errorMessage.toString());
            }
            return ImportResult.success();
        } catch (Exception e) {
            log.error("导入数据源失败: {}", e.getMessage(), e);
            return ImportResult.failure("导入失败: " + e.getMessage());
        }
    }

    /**
     * 验证JSON格式和版本号
     *
     * @param jsonContent JSON内容
     * @param fileName    文件名
     * @return 是否验证通过
     */
    private boolean validateJsonAndVersion(String jsonContent, String fileName) {
        try {
            cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(jsonContent);
            String version = jsonObject.getStr("version");
            String data = jsonObject.getStr("data");

            if (version == null || VersionComparator.INSTANCE.compare(version, "v4.0.5") < 0) {
                log.warn("文件 {} 版本号 {} 不符合要求(需要>=v4.0.5)", fileName, version);
                return false;
            }

            if (data == null || !data.trim().startsWith("[")) {
                log.warn("文件 {} data字段格式错误", fileName);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("文件 {} JSON解析失败: {}", fileName, e.getMessage());
            return false;
        }
    }

    /**
     * 根据文件名导入对应的JSON数据（返回详细结果）
     *
     * @param fileName    文件名
     * @param jsonContent JSON内容
     * @return 导入结果
     */
    private ImportResult importJsonFileWithDetail(String fileName, String jsonContent) {
        try {
            cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(jsonContent);
            String data = jsonObject.getStr("data");
            String type = jsonObject.getStr("type");

            if (BackupType.TEAM.name().equals(type)) {
                return uploadTeamJsonWithDetail(data);
            } else if (BackupType.SQL_TEXT.name().equals(type)) {
                return uploadSqlTextJsonWithDetail(data);
            } else if (BackupType.DATA_SOURCE.name().equals(type)) {
                return uploadDataSourceJsonWithDetail(data);
            } else if (BackupType.BIG_DATA.name().equals(type)) {
                return uploadBigDataJsonWithDetail(data);
            } else {
                return ImportResult.failure("未知的文件类型: " + type);
            }
        } catch (Exception e) {
            return ImportResult.failure("JSON解析失败: " + e.getMessage());
        }
    }

    /**
     * 上传大数据任务（返回详细结果）
     *
     * @param file JSON数据内容
     * @return 导入结果
     */
    public ImportResult uploadBigDataJsonWithDetail(String file) {
        try {
            JSONArray bigDataJson = JSONUtil.parseArray(file);
            if (bigDataJson.isEmpty()) {
                return ImportResult.failure("没有解析出大数据任务json数据，请检查！");
            }

            StringBuilder errorMessage = new StringBuilder();
            boolean hasError = false;
            int successCount = 0;

            for (int i = 0; i < bigDataJson.size(); i++) {
                try {
                    Object object = bigDataJson.get(i);
                    Map<String, Object> map = (Map<String, Object>) object;
                    BigDataTaskModel model = new BigDataTaskModel();
                    model.setTaskName(MapUtil.getStr(map, "taskName"));
                    model.setTaskType(MapUtil.getStr(map, "teamType"));
                    model.setDescription(Base64Decoder.decodeStr(MapUtil.getStr(map, "description")));
                    model.setSqlContent(Base64Decoder.decodeStr(MapUtil.getStr(map, "sqlContent")));
                    model.setCron(MapUtil.getStr(map, "cron"));
                    model.setStatus(MapUtil.getStr(map, "status"));
                    model.setTeamId(MapUtil.getLong(map, "teamId"));
                    Long teamId = model.getTeamId();
                    List<TeamSourceModel> teamSourceModels = teamSourceService.queryTeamByIds(Collections.singletonList(teamId));
                    if (teamSourceModels.isEmpty()) {
                        teamId = Objects.requireNonNull(StpUtils.getCurrentActiveTeam()).getId();
                        model.setTeamId(teamId);
                    }

                    bigDataService.saveTask(model);
                    successCount++;
                    log.info("[{}]大数据任务上传成功!", model.getTaskName());
                } catch (Exception e) {
                    hasError = true;
                    String taskName = "未知任务";
                    try {
                        Map<String, Object> map = (Map<String, Object>) bigDataJson.get(i);
                        taskName = MapUtil.getStr(map, "taskName");
                    } catch (Exception ex) {
                    }
                    errorMessage.append("第").append(i + 1).append("条数据[").append(taskName)
                            .append("]上传失败: ").append(e.getMessage()).append("; ");
                }
            }

            log.info("导入大数据任务{}条数据，成功{}条，失败{}条", bigDataJson.size(), successCount,
                    bigDataJson.size() - successCount);

            if (hasError) {
                return ImportResult.failure("部分大数据任务导入失败: " + errorMessage.toString());
            }
            return ImportResult.success();
        } catch (Exception e) {
            log.error("导入大数据任务失败: {}", e.getMessage(), e);
            return ImportResult.failure("导入失败: " + e.getMessage());
        }
    }

    /**
     * 清理临时目录
     *
     * @param tempDir 临时目录
     */
    private void cleanupTempDirectory(Path tempDir) {
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                                log.debug("已删除临时文件: {}", path.getFileName());
                            } catch (IOException e) {
                                log.warn("删除临时文件失败: {}", e.getMessage());
                            }
                        });
                Files.deleteIfExists(tempDir);
                log.debug("已删除临时目录: {}", tempDir.getFileName());
            } catch (IOException e) {
                log.warn("清理临时目录失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 上传团队信息（返回详细结果）
     *
     * @param file JSON数据内容
     * @return 导入结果
     */
    public ImportResult uploadTeamJsonWithDetail(String file) {
        try {
            JSONArray dataSourceJson = JSONUtil.parseArray(new String(file.getBytes(), StandardCharsets.UTF_8));
            if (dataSourceJson.isEmpty()) {
                return ImportResult.failure("没有解析出团队信息json数据，请检查！");
            }

            StringBuilder errorMessage = new StringBuilder();
            boolean hasError = false;
            int successCount = 0;

            for (int i = 0; i < dataSourceJson.size(); i++) {
                try {
                    Object object = dataSourceJson.get(i);
                    Map<String, Object> map = (Map<String, Object>) object;
                    TeamSourceModel teamSourceModel = new TeamSourceModel();
                    teamSourceModel.setTeamName(Base64Decoder.decodeStr(MapUtil.getStr(map, "teamName")));
                    teamSourceModel.setUserId(MapUtil.getLong(map, "userId"));
                    teamSourceModel.setDescription(Base64Decoder.decodeStr(MapUtil.getStr(map, "description")));
                    teamSourceModel.setState(MapUtil.getInt(map, "state"));
                    teamSourceModel.setId(MapUtil.getLong(map, "id"));

                    AjaxResult ajaxResult = teamSourceService.addTeamSource(teamSourceModel);
                    if (!ajaxResult.getCode().equals(200)) {
                        hasError = true;
                        errorMessage.append("第").append(i + 1).append("条数据[").append(teamSourceModel.getTeamName())
                                .append("]保存失败: ").append(ajaxResult.getMsg()).append("; ");
                    } else {
                        successCount++;
                    }
                } catch (Exception e) {
                    hasError = true;
                    String teamName = "未知团队";
                    try {
                        Map<String, Object> map = (Map<String, Object>) dataSourceJson.get(i);
                        teamName = Base64Decoder.decodeStr(MapUtil.getStr(map, "teamName"));
                    } catch (Exception ex) {
                        // 忽略获取团队名称的异常
                    }
                    errorMessage.append("第").append(i + 1).append("条数据[").append(teamName)
                            .append("]处理失败: ").append(e.getMessage()).append("; ");
                }
            }

            log.info("导入团队信息{}条数据，成功{}条，失败{}条", dataSourceJson.size(), successCount,
                    dataSourceJson.size() - successCount);

            if (hasError) {
                return ImportResult.failure("部分团队信息导入失败: " + errorMessage.toString());
            }
            return ImportResult.success();
        } catch (Exception e) {
            log.error("导入团队信息失败: {}", e.getMessage(), e);
            return ImportResult.failure("导入失败: " + e.getMessage());
        }
    }

    /**
     * 清理临时文件和目录
     *
     * @param backupDir 备份目录
     * @param zipFile   ZIP文件
     */
    private void cleanupTempFiles(Path backupDir, Path zipFile) {
        try {
            if (ObjectUtil.isNotNull(zipFile)) {
                Files.deleteIfExists(zipFile);
            }
            log.debug("已删除临时ZIP文件: {}", zipFile.getFileName());
        } catch (IOException e) {
            log.warn("删除临时ZIP文件失败: {}", e.getMessage());
        }
        try {
            if (Files.exists(backupDir)) {
                Files.walk(backupDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                                log.debug("已删除临时文件: {}", path.getFileName());
                            } catch (IOException e) {
                                log.warn("删除临时文件失败: {}", e.getMessage());
                            }
                        });
                Files.deleteIfExists(backupDir);
                log.debug("已删除临时备份目录: {}", backupDir.getFileName());
            }
        } catch (IOException e) {
            log.warn("清理临时备份目录失败: {}", e.getMessage());
        }
    }


    /**
     * 导入结果封装类
     */
    private static class ImportResult {
        private final boolean success;
        private final String errorMessage;

        public ImportResult(boolean success) {
            this(success, null);
        }

        public ImportResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static ImportResult success() {
            return new ImportResult(true);
        }

        public static ImportResult failure(String errorMessage) {
            return new ImportResult(false, errorMessage);
        }
    }
}
