package com.hd.forum.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.forum.common.Result;
import com.hd.forum.common.annotation.RequireSuperAdmin;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.entity.Tag;
import com.hd.forum.mapper.TagMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员标签管理接口。
 *
 * 说明：
 * - 本 Controller 面向后台管理功能：标签分页、创建、修改、删除
 * - 权限通过 @RequireSuperAdmin 保护
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理(管理员)")
@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagMapper tagMapper;

    public record TagUpsertRequest(
            Integer id,
            @NotBlank(message = "标签名不能为空") String name
    ) {
    }

    @RequireSuperAdmin
    @Operation(summary = "分页查询标签")
    @GetMapping("/page")
    public com.hd.forum.common.Result<Page<Tag>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null && !keyword.trim().isEmpty(), Tag::getName, keyword.trim());
        wrapper.orderByDesc(Tag::getCreateTime).orderByDesc(Tag::getId);
        return com.hd.forum.common.Result.success(tagMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @RequireSuperAdmin
    @Operation(summary = "新增标签")
    @PostMapping("/create")
    public com.hd.forum.common.Result<String> create(@RequestBody @Valid TagUpsertRequest req) {
        Tag tag = new Tag();
        tag.setName(req.name().trim());
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("标签已存在");
        }
        return com.hd.forum.common.Result.success("创建成功");
    }

    @RequireSuperAdmin
    @Operation(summary = "修改标签")
    @PutMapping("/update")
    public com.hd.forum.common.Result<String> update(@RequestBody @Valid TagUpsertRequest req) {
        if (req.id() == null) {
            throw new BusinessException("标签ID不能为空");
        }
        Tag tag = tagMapper.selectById(req.id());
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        tag.setName(req.name().trim());
        try {
            tagMapper.updateById(tag);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("标签已存在");
        }
        return com.hd.forum.common.Result.success("修改成功");
    }

    @RequireSuperAdmin
    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    @Transactional
    public com.hd.forum.common.Result<String> delete(@PathVariable Integer id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        
        // 1. 删除标签主表记录
        tagMapper.deleteById(id);
        
        return com.hd.forum.common.Result.success("删除成功");
    }
}
