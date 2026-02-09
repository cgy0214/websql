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

    @Column
    private String taskName;

    @Column
    private String taskType;

    @Column
    private String description;

    @Column
    private String cron;

    @Column(columnDefinition = "TEXT")
    private String sqlContent;

    /**
     * 状态,未发布,已发布
     */
    @Column
    private String status;

    @Column
    private Long teamId;

    @Column
    private String createUser;

    @Column
    private String updateUser;

    @Column
    private String createTime;

    @Column
    private String updateTime;

    /**
     * 发布/撤回时间
     */
    @Column
    private String releaseTime;

    /**
     * 发布/撤回人
     */
    @Column
    private String releaseUser;
}
