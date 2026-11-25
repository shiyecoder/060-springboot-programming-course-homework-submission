package com.grad.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.code.entity.Course;
import org.apache.ibatis.annotations.Mapper;

/** 课程 Mapper */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
