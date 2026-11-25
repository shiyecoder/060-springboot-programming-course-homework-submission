package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩汇总
 */
@Data
@TableName("score_summary")
public class ScoreSummary {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业 code_assignment.id */
    private Long assignmentId;

    /** 学生 sys_user.id */
    private Long studentId;

    /** 运行结果分 = 通过数/总数*100 */
    private BigDecimal autoScore;

    /** 互评分（按可信度加权后） */
    private BigDecimal peerScore;

    /** 互评原始均分（未折算，便于复核） */
    private BigDecimal peerRawScore;

    /** 教师评分 0.0-100.0 可空 */
    private BigDecimal teacherScore;

    /** 最终成绩 三项按权重合成 */
    private BigDecimal finalScore;

    /** 班级排名 */
    private Integer classRank;

    /** 互评可信度分 */
    private BigDecimal credibilityScore;

    /** 可信度等级 高可信/中可信/较低可信/低可信 */
    private String credibilityGrade;

    /** 人工调整说明（≥10字） */
    private String adjustReason;

    /** 发布状态 未发布/已发布/已撤回 */
    private String publishStatus;

    /** 发布时间 */
    private LocalDateTime publishTime;

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
