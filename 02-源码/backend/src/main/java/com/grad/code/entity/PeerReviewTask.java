package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 互评任务
 */
@Data
@TableName("peer_review_task")
public class PeerReviewTask {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号 HP+yyMMdd+5位序号 */
    private String taskNo;

    /** 作业 code_assignment.id */
    private Long assignmentId;

    /** 评阅人 sys_user.id，不能等于被评提交作者 */
    private Long reviewerId;

    /** 被评提交 code_submission.id */
    private Long submissionId;

    /** 分配轮次 1首次 2补评 3重新互评 */
    private Integer assignRound;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 是否匿名 1匿名 0实名 */
    private Integer isAnonymous;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 提醒次数（截止前2小时催评） */
    private Integer remindCount;

    /** 状态 待评/已评/超时未评/已豁免 */
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
