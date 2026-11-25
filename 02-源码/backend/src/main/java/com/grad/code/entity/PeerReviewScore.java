package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 互评评分
 */
@Data
@TableName("peer_review_score")
public class PeerReviewScore {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 互评任务 peer_review_task.id（一任务一条） */
    private Long taskId;

    /** 评阅人 sys_user.id */
    private Long reviewerId;

    /** 被评提交 code_submission.id */
    private Long submissionId;

    /** 正确性得分 0.0-60.0 */
    private BigDecimal correctnessScore;

    /** 可读性得分 0.0-25.0 */
    private BigDecimal readabilityScore;

    /** 规范得分 0.0-15.0 */
    private BigDecimal styleScore;

    /** 互评总分 三项之和 0.0-100.0 */
    private BigDecimal totalScore;

    /** 评语 不少于20字，需指出具体代码位置 */
    private String comment;

    /** 评语字数（去空白） */
    private Integer commentLength;

    /** 是否低质量 1套话或字数不足 */
    private Integer lowQuality;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 状态 草稿/已提交/已隐藏 */
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
