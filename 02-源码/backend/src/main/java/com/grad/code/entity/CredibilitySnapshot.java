package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可信度留痕
 */
@Data
@TableName("credibility_snapshot")
public class CredibilitySnapshot {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快照编号 CT+yyMMdd+6位序号 */
    private String snapshotNo;

    /** 互评任务 peer_review_task.id */
    private Long taskId;

    /** 互评评分 peer_review_score.id */
    private Long scoreId;

    /** 被评提交 code_submission.id */
    private Long submissionId;

    /** 被评学生 sys_user.id */
    private Long studentId;

    /** 作业 code_assignment.id */
    private Long assignmentId;

    /** 计算所用规则版本 */
    private String ruleVersion;

    /** 评分偏离度得分 */
    private BigDecimal sDeviation;

    /** 评语有效长度得分 */
    private BigDecimal sComment;

    /** 互评完成率得分 */
    private BigDecimal sFinishRate;

    /** 提交相似度得分 */
    private BigDecimal sSimilarity;

    /** 评分一致性得分 */
    private BigDecimal sConsistency;

    /** 可信度总分 Σ(得分×权重) */
    private BigDecimal totalScore;

    /** 结果等级 高可信/中可信/较低可信/低可信 */
    private String grade;

    /** 输入快照 JSON（计算时冻结） */
    private String inputJson;

    /** 结论说明（互评分如何计入成绩） */
    private String conclusion;

    /** 是否人工修正 0否 1是 */
    private Integer manualFlag;

    /** 人工修正原因（≥5字） */
    private String manualReason;

    /** 生成时间 */
    private LocalDateTime calcTime;

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
