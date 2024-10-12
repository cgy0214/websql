package com.itboy.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Accessors(chain = true)
@Data
@Entity
@Table(name = "SYS_DETECTION_INFO")
public class SysDetectionModel extends Pages{

    @Id
    @GenericGenerator(name = "generator", strategy = "native")
    @GeneratedValue(generator = "generator")
    private Long id;


    /***
     * 团队ID
     */
    @Column(nullable = false)
    private Long teamId;

    /**
     * 监测任务名称
     */
    private String name;

    /**
     * 执行数据源名称
     */
    private String dataBaseName;

    /**
     * 执行sql
     */
    private String sqlContent;

    /**
     * 返回值列名
     */
    private String columnName;

    /**
     * 计算表达式
     */
    private String expression;

    /**
     * 告警级别，用于推送消息
     */
    private String alarmLevel;

    /**
     * 执行时间
     */
    private String cron;

    /**
     * 状态
     */
    private String state;

    /**
     * 创建时间
     */
    private Date createTime;

    private String createUser;
}
