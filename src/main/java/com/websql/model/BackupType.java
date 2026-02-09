package com.websql.model;

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
