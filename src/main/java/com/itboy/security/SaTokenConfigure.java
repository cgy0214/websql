package com.itboy.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.itboy.util.StpUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * @ClassName : SaTokenConfigure
 * @Description : 权限控制器
 * @Author 超 boy_0214@sina.com
 * @Date: 2023/1/28 23:35
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {

            SaRouter.match("/**")
                    .notMatch("/login/**")
                    .notMatch("/code/**")
                    .notMatch("/common/**")
                    .notMatch("/statics/**")
                    .notMatch("/static/**")
                    .notMatch("/login")
                    .notMatch("/error")
                    .notMatch("/openApiManager/**")
                    .notMatch("/unlockLoginUser")
                    .check(r -> StpUtil.checkLogin());

            List<String> list = Arrays.asList("/dataSourceManager/page",
                    "/dataSourceManager/dataSourceList",
                    "/sqlManager/sqlPage",
                    "/dataSourceManager/findDataSourceList",
                    "/sqlManager/querySqlTextSelect",
                    "/sqlManager/findTableField",
                    "/sqlManager/executeSqlNew",
                    "/sqlManager/sqlTextPage",
                    "/sqlManager/querySqlTextList",
                    "/sqlManager/saveSqlText",
                    "/timingManager/listPage",
                    "/timingManager/addPage",
                    "/timingManager/addTimingData",
                    "/timingManager/historyPage",
                    "/timingManager/jobLogList",
                    "/timingManager/timingList",
                    "/logManager/logPage",
                    "/logManager/getLogList",
                    "/logManager/userLogPage",
                    "/logManager/getUserLogList",
                    "/settingManager/driverConfigPage",
                    "/settingManager/driverConfigList");
            SaRouter.match(list).check(r -> {
                if (StpUtil.hasRole("demo-admin")) {
                    r.stop();
                }
            });

            SaRouter.match("/dataSourceManager/**")
                    .notMatch("/dataSourceManager/findDataSourceList")
                    .check(r -> StpUtil.checkRoleOr("database-admin", "super-admin"));
            SaRouter.match("/logManager/**", r -> StpUtil.checkRoleOr("log-admin", "super-admin"));
            SaRouter.match("/sqlManager/**", r -> StpUtil.checkRoleOr("sql-admin", "super-admin"));
            SaRouter.match("/timingManager/**", r -> StpUtil.checkRoleOr("timing-admin", "super-admin"));
            SaRouter.match("/settingManager/**", r -> StpUtil.checkRoleOr("super-admin"));
            SaRouter.match("/sshManager/**", r -> StpUtil.checkRoleOr("super-admin"));
            SaRouter.match("/openApiManager/**", r -> StpUtils.checkOpenAuth());
            SaRouter.match("/detectionManager/**", r -> StpUtil.checkRoleOr("timing-admin", "super-admin"));
        })).addPathPatterns("/**");
    }
}
