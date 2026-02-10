package com.websql.model;

/**
 * @ClassName BackupType
 * @Description 备份类型枚举
 * @Author rabbit boy_0214@sina.com
 * @Date 2024/02/10 10:00
 */
public enum BackupType {

    DATA_SOURCE("数据源"),
    SQL_TEXT("SQL列表"),
    TEAM("团队"),
    BIG_DATA("枢易方舟");

    private final String value;


    BackupType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BackupType getByValue(String value) {
        for (BackupType type : BackupType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }

}
