package com.itboy.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.itboy.util.StpUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
            SaRouter.match("/dataSourceManager/**")
                    .notMatch("/dataSourceManager/findDataSourceList")
                    .check(r -> StpUtil.checkRoleOr("database-admin"));
            SaRouter.match("/logManager/**", r -> StpUtil.checkRole("log-admin"));
            SaRouter.match("/sqlManager/**", r -> StpUtil.checkRole("sql-admin"));
            SaRouter.match("/timingManager/**", r -> StpUtil.checkRole("timing-admin"));
            SaRouter.match("/settingManager/**", r -> StpUtil.checkRole("super-admin"));
            SaRouter.match("/openApiManager/**", r -> StpUtils.checkOpenAuth());
        })).addPathPatterns("/**");
    }
}
