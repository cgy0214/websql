package com.websql;

import cn.hutool.core.net.NetUtil;
import com.websql.util.EnvBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @ClassName WEBSQL-PORJECT
 * @Description 网页版执行SQL
 * @Author rabbit boy_0214@sina.com
 **/
@SpringBootApplication
@EnableJpaAuditing
@Slf4j
@ServletComponentScan
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.info("WELCOME WEBSQL-PROJECT http://{}:{}", NetUtil.getLocalhostStr(), EnvBeanUtil.getString("server.port"));
    }


}
