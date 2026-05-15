package com.hd.forum.controller;

import com.hd.forum.common.Result;
import com.hd.forum.service.IBoardService;
import com.hd.forum.vo.BoardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 板块（分区）相关的公开接口。
 *
 * 主要用于前台展示板块列表，并返回板主信息（如果存在）。
 */
@Tag(name = "板块接口")
@RestController
@RequestMapping("/api/public/board")
@RequiredArgsConstructor
public class BoardController {

    private final IBoardService boardService;

    @Operation(summary = "获取板块列表(包含板主信息)")
    @GetMapping("/list")
    public Result<List<BoardVO>> list() {
        List<BoardVO> list = boardService.listWithModerator();
        return Result.success(list);
    }
}
