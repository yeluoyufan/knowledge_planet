package com.hd.forum.controller;

import com.hd.forum.common.Result;
import com.hd.forum.entity.Tag;
import com.hd.forum.mapper.TagMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共标签接口（前台使用）。
 *
 * 用途：
 * - 发帖页面展示可选标签
 * - 搜索/筛选时展示标签列表
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "TagController", description = "标签接口")
@RestController
@RequestMapping("/api/public/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagMapper tagMapper;

    @Operation(summary = "获取所有标签")
    @GetMapping("/all")
    public Result<List<Tag>> listAll() {
        return Result.success(tagMapper.selectList(null));
    }
}
