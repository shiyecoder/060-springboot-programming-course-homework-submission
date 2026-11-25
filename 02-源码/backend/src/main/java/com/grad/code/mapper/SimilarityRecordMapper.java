package com.grad.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.code.entity.SimilarityRecord;
import org.apache.ibatis.annotations.Mapper;

/** 查重记录 Mapper */
@Mapper
public interface SimilarityRecordMapper extends BaseMapper<SimilarityRecord> {
}
