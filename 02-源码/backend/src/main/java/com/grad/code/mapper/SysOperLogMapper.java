package com.grad.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.code.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志 Mapper */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
