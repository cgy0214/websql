package com.websql.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.websql.model.SysUser;
import com.websql.model.TeamSourceModel;
import com.websql.service.LoginService;
import com.websql.service.TeamSourceService;
import com.websql.util.StpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @ClassName : SaTokenConfigure
 * @Description : 权限控制器
 * @Author rabbit boy_0214@sina.com
 * @Date: 2023/1/28 23:35
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {


    @Value("${server.dev.mode}")
    private boolean devMode;

    @Autowired
    private LoginService loginService;

    @Resource
    private TeamSourceService teamSourceService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {

            if (devMode) {
                if (!StpUtil.isLogin()) {
                    SysUser user = loginService.findUserById(1L);
                    List<TeamSourceModel> teamList = teamSourceService.queryValidTeamList();
                    StpUtils.login(user, teamList);
                }
                SaRouter.notMatch("/**")
                        .check(r -> StpUtil.checkLogin());
                return;
            }

            // 全局无需鉴权的路径
            SaRouter.match("/**")
                    .notMatch("/")
                    .notMatch("/login/**")
                    .notMatch("/code/**")
                    .notMatch("/common/**")
                    .notMatch("/statics/**")
                    .notMatch("/static/**")
                    .notMatch("/login")
                    .notMatch("/error")
                    .notMatch("/openApiManager/**")
                    .notMatch("/unlockLoginUser")
                    .notMatch("/favicon.ico")
                    .check(r -> StpUtil.checkLogin());

            // DEMO 用户权限限制 - 只能访问特定路径
            SaRouter.match(demoAllowUrl()).check(r -> {
                if (StpUtil.hasRole("demo-admin")) {
                    r.stop();
                }
            });

            // 开放接口权限验证
            SaRouter.match("/openApiManager/**")
                    .check(r -> StpUtils.checkOpenAuth());

            // 数据源管理权限 - database-admin 或 super-admin 可访问，但排除公开接口
            SaRouter.match("/dataSourceManager/**")
                    .notMatch("/dataSourceManager/findDataSourceList") // 公开的数据源API
                    .check(r -> StpUtil.checkRoleOr("database-admin", "super-admin"));

            // 系统设置权限 - 仅 super-admin 可访问，但排除公开接口
            SaRouter.match("/settingManager/**")
                    .notMatch("/settingManager/findMessageTemplateList") // 公开的告警模板API
                    .check(r -> StpUtil.checkRoleOr("super-admin"));

            // 日志管理权限 - log-admin 或 super-admin 可访问
            SaRouter.match("/logManager/**")
                    .check(r -> StpUtil.checkRoleOr("log-admin", "super-admin"));

            // SQL管理权限 - sql-admin 或 super-admin 可访问
            SaRouter.match("/sqlManager/**")
                    .check(r -> StpUtil.checkRoleOr("sql-admin", "super-admin"));

            // 定时任务管理权限 - timing-admin 或 super-admin 可访问
            SaRouter.match("/timingManager/**")
                    .check(r -> StpUtil.checkRoleOr("timing-admin", "super-admin"));

            // SSH管理权限 - 仅 super-admin 可访问
            SaRouter.match("/sshManager/**")
                    .check(r -> StpUtil.checkRoleOr("super-admin"));

            // 检测管理权限 - timing-admin 或 super-admin 可访问
            SaRouter.match("/detectionManager/**")
                    .check(r -> StpUtil.checkRoleOr("timing-admin", "super-admin"));

            // AI管理权限 - sql-admin、timing-admin 或 super-admin 可访问
            SaRouter.match("/aiManager/**")
                    .check(r -> StpUtil.checkRoleOr("sql-admin", "timing-admin", "super-admin"));

        })).addPathPatterns("/**");
    }

    /**
     * 演示角色可以访问的功能url
     *
     * @return 允许演示用户访问的URL列表
     */
    private List<String> demoAllowUrl() {
        return Arrays.asList(
                // 数据源相关
                "/dataSourceManager/page",
                "/dataSourceManager/dataSourceList",
                "/dataSourceManager/findDataSourceList",

                // SQL相关
                "/sqlManager/sqlPage",
                "/sqlManager/querySqlTextSelect",
                "/sqlManager/findTableField",
                "/sqlManager/executeSqlNew",
                "/sqlManager/sqlTextPage",
                "/sqlManager/querySqlTextList",
                "/sqlManager/saveSqlText",
                "/sqlManager/metaTablePage",
                "/sqlManager/metaTreeTableList",
                "/sqlManager/createAsyncExport",
                "/sqlManager/queryExportData",
                "/sqlManager/downloadExcelFile",

                // 定时任务相关
                "/timingManager/listPage",
                "/timingManager/addPage",
                "/timingManager/addTimingData",
                "/timingManager/historyPage",
                "/timingManager/jobLogList",
                "/timingManager/timingList",

                // 日志相关
                "/logManager/logPage",
                "/logManager/getLogList",
                "/logManager/userLogPage",
                "/logManager/getUserLogList",

                // 系统设置相关
                "/settingManager/driverConfigPage",
                "/settingManager/driverConfigList",
                "/settingManager/teamManagerPage",
                "/settingManager/showTeamResourcePage/**",
                "/settingManager/queryTeamList",
                "/settingManager/queryTeamResourceList",
                "/settingManager/exportLogPage",
                "/settingManager/exportFilesLogList",

                // 检测相关
                "/detectionManager/listPage",
                "/detectionManager/addPage",
                "/detectionManager/reportPage",
                "/detectionManager/list",
                "/detectionManager/findDataSelect",
                "/detectionManager/logList",
                "/detectionManager/logCharts",
                //ai 功能
                "/aiManager/*"
        );
    }

}