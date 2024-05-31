package com.itboy;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * @ClassName WebSqlServletInitializer
 * @Description 网页版执行SQL
 * @Author 超 boy_0214@sina.com
 **/
public class WebSqlServletInitializer extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }
}
