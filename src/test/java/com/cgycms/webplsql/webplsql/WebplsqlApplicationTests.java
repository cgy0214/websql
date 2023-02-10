package com.cgycms.webplsql.webplsql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
//缺少SpringBootConfiguration 的话test异常，可手动打包跳过test
@SpringBootConfiguration
public class WebplsqlApplicationTests {

    @Test
    public void contextLoads() {
    }

}
