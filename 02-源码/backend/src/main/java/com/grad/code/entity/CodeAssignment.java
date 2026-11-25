package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 编程作业
 */
@Data
@TableName("code_assignment")
public class CodeAssignment {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业编号 ZY+yyMMdd+3位序号 */
    private String assignmentNo;

    /** 所属课程 course.id */
    private Long courseId;

    /** 标题 同课程同周次不可重复 */
    private String title;

    /** 题目描述 不少于30字，含输入输出说明 */
    private String description;

    /** 编程语言 */
    private String languageType;

    /** 发布时间 草稿为空 */
    private LocalDateTime publishTime;

    /** 截止时间 必须晚于发布时间 */
    private LocalDateTime deadline;

    /** 迟交截止 最多晚于截止7天 */
    private LocalDateTime lateDeadline;

    /** 互评截止时间（补充字段，互评任务默认取此值） */
    private LocalDateTime reviewDeadline;

    /** 用例数量 1-30 */
    private Integer testCaseCount;

    /** 互评份数 0表示不互评 1-5 */
    private Integer peerReviewCount;

    /** 成绩权重 0.00-100.00 */
    private BigDecimal weight;

    /** 运行结果分权重 三项合计必须100 */
    private Integer runWeight;

    /** 互评分权重 三项合计必须100 */
    private Integer peerWeight;

    /** 教师评分权重 三项合计必须100 */
    private Integer teacherWeight;

    /** 状态 草稿/已发布/提交中/互评中/评分中/已归档/已撤回 */
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
