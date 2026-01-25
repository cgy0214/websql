package com.websql.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "BIGDATA_TASK")
@Data
public class BigDataTaskModel extends Pages implements Serializable {

    @Id
    @GenericGenerator(name = "generator", strategy = "native")
    @GeneratedValue(generator = "generator")
    private Long id;

    @Column(nullable = false)
    private String taskName;

    @Column
    private String taskType;

    @Column
    private String taskCode;

    @Column
    private String description;

    @Column
    private String sqlContent;

    @Column
    private String dataSource;

    @Column
    private String status;

    @Column
    private Long teamId;

    @Column
    private String createUser;

    @Column
    private String createTime;

    @Column
    private String updateTime;
}
