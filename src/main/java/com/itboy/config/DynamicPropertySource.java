package com.itboy.config;

/**
 * @ClassName DynamicPropertySource
 * @Description 动态修改环境变量
 * @Author 超 boy_0214@sina.com
 * @Date 2024/4/10 15:40
 **/
public class DynamicPropertySource extends org.springframework.core.env.PropertySource<Object> {
    private final String key;
    private final String value;

    public DynamicPropertySource(String key, String value) {
        super("DynamicPropertySource");
        this.key = key;
        this.value = value;
    }

    @Override
    public Object getProperty(String name) {
        return (name.equals(key)) ? value : null;
    }
}
