package com.hd.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.forum.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 上传文件元信息表（sys_file）数据访问层（Mapper）。
 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
}
