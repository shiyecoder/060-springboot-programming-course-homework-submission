package com.grad.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩申诉
 */
@Data
@TableName("appeal_record")
public class AppealRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申诉编号 SS+yyMMdd+4位序号 */
    private String appealNo;

    /** 成绩记录 score_summary.id */
    private Long scoreId;

    /** 学生 sys_user.id */
    private Long studentId;

    /** 申诉类型 互评分偏低/查重误判/成绩计算错误 */
    private String appealType;

    /** 申诉理由 不少于20字 */
    private String reason;

    /** 期望分数（可空） */
    private BigDecimal expectScore;

    /** 证据附件相对路径，最多5个 */
    private String evidenceUrls;

    /** 处理状态 待受理/处理中/已驳回/已改分/需重新互评 */
    private String status;

    /** 处理人 sys_user.id（不能是原评分教师） */
    private Long handlerId;

    /** 处理结果 改分需写明新分数与依据 */
    private String handleResult;

    /** 处理时间 */
    private LocalDateTime handleTime;

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
