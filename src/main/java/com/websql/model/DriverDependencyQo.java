package com.websql.model;

import lombok.Data;


@Data
public class DriverDependencyQo {

    private String groupId;
    private String artifactId;
    private String version;

    private String xmlContent;
    private String type;
    private String central;

    public DriverDependencyQo(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
}
