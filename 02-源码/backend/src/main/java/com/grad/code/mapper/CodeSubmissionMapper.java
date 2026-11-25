package com.grad.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.code.entity.CodeSubmission;
import org.apache.ibatis.annotations.Mapper;

/** 代码提交 Mapper */
@Mapper
public interface CodeSubmissionMapper extends BaseMapper<CodeSubmission> {
}
