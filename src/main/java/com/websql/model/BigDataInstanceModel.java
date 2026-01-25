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

    @Column
    private String instanceStatus;

    @Column
    private String startTime;

    @Column
    private String endTime;

    @Column
    private String executeResult;

    @Column
    private String errorMessage;

    @Column
    private Long executeTime;

    @Column
    private String createUser;

    @Column
    private String createTime;
}
