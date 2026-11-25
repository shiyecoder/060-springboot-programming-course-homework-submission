package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交版本
 */
@Data
@TableName("code_submission_version")
public class CodeSubmissionVersion {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 版本编号 BB+提交编号+2位序号 */
    private String versionNo;

    /** 提交 code_submission.id */
    private Long submissionId;

    /** 作业 code_assignment.id */
    private Long assignmentId;

    /** 学生 sys_user.id */
    private Long studentId;

    /** 第几次提交 1 起 */
    private Integer seqNo;

    /** 语言快照 */
    private String codeLanguage;

    /** 代码行数 */
    private Integer codeLines;

    /** 文件大小（字节） */
    private Integer fileSize;

    /** 归一化后指纹 */
    private String codeDigest;

    /** 该版本代码正文 */
    private String codeText;

    /** 是否迟交 */
    private Integer isLate;

    /** 提交时间 */
    private LocalDateTime submitTime;

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
