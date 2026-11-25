package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代码提交
 */
@Data
@TableName("code_submission")
public class CodeSubmission {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交编号 TD+yyMMdd+5位序号 */
    private String submitNo;

    /** 作业 code_assignment.id */
    private Long assignmentId;

    /** 学生 sys_user.id */
    private Long studentId;

    /** 源文件相对路径 禁止外链 */
    private String filePath;

    /** 源文件大小（字节）≤204800 */
    private Integer fileSize;

    /** 代码正文快照（≤64KB，互评展示与版本对比用） */
    private String codeText;

    /** 提交时的语言快照 */
    private String codeLanguage;

    /** 代码行数 1-2000 */
    private Integer codeLines;

    /** 代码指纹（归一化后 SHA-1，相似度比对用） */
    private String codeDigest;

    /** 提交时间 以服务器时间为准 */
    private LocalDateTime submitTime;

    /** 提交次数 1-20 */
    private Integer submitCount;

    /** 编译结果 未运行/通过/编译失败 */
    private String compileResult;

    /** 用例通过数 0-作业用例数 */
    private Integer passCount;

    /** 用例总数（提交时快照） */
    private Integer totalCount;

    /** 编译错误摘要（编译失败必填） */
    private String errorSummary;

    /** 是否迟交 0按时 1迟交 */
    private Integer isLate;

    /** 状态 待登记/已登记/已进入互评/已评分 */
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
