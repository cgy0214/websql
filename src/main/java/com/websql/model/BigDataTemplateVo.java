package com.websql.model;

import lombok.Data;

import java.util.List;

@Data
public class BigDataTemplateVo {

    private String code;

    private String title;

    private List<String> content;

    private String description;

    private String helpUrl;

    private String version;
}
