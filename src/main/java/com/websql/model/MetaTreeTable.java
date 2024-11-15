package com.websql.model;

import lombok.Data;

import java.util.List;

@Data
public class MetaTreeTable {

    private String title;

    private String id;

    private String field;

    private boolean disabled;

    private DataSourceMeta tableMeta;

    private List<MetaTreeTable> children;

}
