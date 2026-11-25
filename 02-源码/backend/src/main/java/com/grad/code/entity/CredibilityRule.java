package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可信度规则
 */
@Data
@TableName("credibility_rule")
public class CredibilityRule {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则版本 v2.1 */
    private String ruleVersion;

    /** 规则名称 */
    private String ruleName;

    /** 评分偏离度权重 % */
    private BigDecimal weightDeviation;

    /** 评语有效长度权重 % */
    private BigDecimal weightComment;

    /** 互评完成率权重 % */
    private BigDecimal weightFinishRate;

    /** 提交相似度权重 % */
    private BigDecimal weightSimilarity;

    /** 评分一致性权重 % */
    private BigDecimal weightConsistency;

    /** 高可信阈值 默认85 */
    private BigDecimal gradeHigh;

    /** 中可信阈值 默认65 */
    private BigDecimal gradeMid;

    /** 较低可信阈值 默认45 */
    private BigDecimal gradeLow;

    /** 相似度告警线 % 默认70 */
    private BigDecimal similarityAlert;

    /** 生效时间 */
    private LocalDateTime effectTime;

    /** 修改人 sys_user.id */
    private Long operatorId;

    /** 状态 生效中/已失效 */
    private String status;

    /** 变更说明 */
    private String remark;

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
