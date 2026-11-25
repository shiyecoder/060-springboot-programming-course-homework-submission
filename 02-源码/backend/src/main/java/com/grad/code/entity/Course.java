package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程
 */
@Data
@TableName("course")
public class Course {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程编号 KC+学期码+3位序号 */
    private String courseNo;

    /** 课程名称 如Java程序设计 */
    private String courseName;

    /** 学期 如2026学年第一学期 */
    private String semester;

    /** 开课学院 */
    private String collegeName;

    /** 授课教师 sys_user.id */
    private Long teacherId;

    /** 教学班 如软工2301 */
    private String className;

    /** 学生人数（由选课名单汇总） */
    private Integer studentCount;

    /** 编程语言 Java/Python/C++/C/JavaScript/Go */
    private String languageType;

    /** 作业数量（由作业表汇总） */
    private Integer assignmentCount;

    /** 课程封面相对路径 */
    private String coverImage;

    /** 状态 进行中/已结课/已归档 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;

    /** 创建人 */
    private Long createBy;

    /** 最后修改人 */
    private Long updateBy;

    /** 乐观锁版本号 */
    private Integer version;
}
