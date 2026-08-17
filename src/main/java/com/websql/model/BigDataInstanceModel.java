package com.websql.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "BIGDATA_INSTANCE")
@Data
public class BigDataInstanceModel extends Pages implements Serializable {

    @Id
    @GenericGenerator(name = "generator", strategy = "native")
    @GeneratedValue(generator = "generator")
    private Long id;

    @Column
    private Long taskId;

    @Column
    private String taskName;

    /**
     * 执行状态， 单sql 执行结果，多sql执行结果一条失败就算失败
     */
    @Column
    private String instanceStatus;

    @Column
    private String startTime;

    @Column
    private String endTime;

    @Column(columnDefinition = "text")
    private String executeResult;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column
    private String createUser;

    @Column
    private String createTime;

    @Column(columnDefinition = "text")
    private String sqlContent;

    @Column
    private String taskCreateUser;

    @Column
    private Long taskTeamId;
}
