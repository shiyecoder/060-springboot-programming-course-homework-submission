package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 查重记录
 */
@Data
@TableName("similarity_record")
public class SimilarityRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 记录编号 CD+yyMMdd+4位序号 */
    private String recordNo;

    /** 作业 code_assignment.id */
    private Long assignmentId;

    /** 提交A（取较小ID） */
    private Long submissionAId;

    /** 提交B（取较大ID） */
    private Long submissionBId;

    /** 相似度 0.00-100.00，>70 进入待复核 */
    private BigDecimal similarityRate;

    /** 结构相似度（权重0.5） */
    private BigDecimal structureRate;

    /** 标识符相似度（权重0.3） */
    private BigDecimal identifierRate;

    /** 注释相似度（权重0.2） */
    private BigDecimal commentRate;

    /** 命中类型 变量重命名/结构一致/注释雷同/完全一致 */
    private String matchType;

    /** 证据片段 相同行区间与内容摘要 */
    private String evidenceSnippet;

    /** 证据截图相对路径 */
    private String evidenceImage;

    /** 检测规则版本 v1.2 */
    private String checkRuleVersion;

    /** 检测批次号（重复检测覆盖旧记录时保留） */
    private String batchNo;

    /** 复核结论 待复核/确认抄袭/误报/需面谈 */
    private String reviewResult;

    /** 复核人 sys_user.id */
    private Long reviewerId;

    /** 第二名签字教师（确认抄袭必需） */
    private Long secondReviewerId;

    /** 复核理由 */
    private String reviewRemark;

    /** 复核时间 */
    private LocalDateTime reviewTime;

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
