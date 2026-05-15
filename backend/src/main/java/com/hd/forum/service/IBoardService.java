package com.hd.forum.service;

import com.hd.forum.entity.Board;
import com.baomidou.mybatisplus.extension.service.IService;

import com.hd.forum.dto.BoardRequest;
import com.hd.forum.vo.BoardVO;
import java.util.List;

/**
 * 板块业务服务接口。
 *
 * 板块用于对帖子进行分区管理，并可关联版主信息用于前台展示。
 */
public interface IBoardService extends IService<Board> {

    /**
     * 查询板块列表，并补充板主信息（如果该板块有板主）。
     */
    List<BoardVO> listWithModerator();

    /**
     * 创建板块。
     */
    void createBoard(BoardRequest req);

    /**
     * 更新板块信息（含版主任命）。
     */
    void updateBoard(Integer id, BoardRequest req);

    /**
     * 删除板块。
     */
    void deleteBoard(Integer id);
}
