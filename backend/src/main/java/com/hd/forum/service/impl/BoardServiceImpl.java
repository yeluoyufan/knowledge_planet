package com.hd.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hd.forum.dto.BoardRequest;
import com.hd.forum.entity.Board;
import com.hd.forum.entity.Post;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.BoardMapper;
import com.hd.forum.mapper.PostMapper;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.IBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.utils.SecurityUtils;
import com.hd.forum.vo.BoardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 板块业务服务实现类。
 *
 * 主要负责：
 * - 板块基础信息的增删改查
 * - 补充板主信息（用于前端板块列表展示）
 */
@Service
@RequiredArgsConstructor
public class BoardServiceImpl extends ServiceImpl<BoardMapper, Board> implements IBoardService {
    
    private final UserMapper userMapper;
    private final PostMapper postMapper;

    @Override
    public List<BoardVO> listWithModerator() {
        // 1. 查询所有板块
        List<Board> boards = this.list();
        
        // 2. 统计每个板块的帖子数量 (仅统计已发布且未删除的帖子)
        List<Map<String, Object>> counts = postMapper.selectMaps(new QueryWrapper<Post>()
                .select("board_id", "count(*) as count")
                .eq("status", 1)
                .eq("deleted", 0)
                .groupBy("board_id"));
        
        Map<Integer, Integer> postCountMap = counts.stream()
                .collect(Collectors.toMap(
                        m -> (Integer) m.get("board_id"),
                        m -> ((Long) m.get("count")).intValue()
                ));

        // 3. 查询所有板主 (role = 'MODERATOR')
        List<User> moderators = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "MODERATOR")
                .isNotNull(User::getManagedBoardId));
        
        // 4. 按 boardId 分组板主 (理论上一个板块只有一个板主，这里用 map 方便匹配)
        Map<Integer, User> boardModeratorMap = moderators.stream()
                .collect(Collectors.toMap(User::getManagedBoardId, u -> u, (u1, u2) -> u1));
        
        // 5. 组装 VO 并排序
        return boards.stream().map(board -> {
            BoardVO vo = new BoardVO();
            BeanUtils.copyProperties(board, vo);
            vo.setPostCount(postCountMap.getOrDefault(board.getId(), 0));
            
            User mod = boardModeratorMap.get(board.getId());
            if (mod != null) {
                vo.setModeratorName(mod.getNickname() != null ? mod.getNickname() : mod.getUsername());
                vo.setModeratorId(mod.getId());
            } else {
                vo.setModeratorName("暂无版主");
            }
            return vo;
        })
        .sorted((v1, v2) -> v2.getPostCount().compareTo(v1.getPostCount())) // 帖子多排前面
        .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createBoard(BoardRequest req) {
        Board board = new Board();
        board.setName(req.name());
        board.setDescription(req.description());
        Integer sort = req.sort();
        if (sort == null) {
            Board lastBoard = this.getOne(new LambdaQueryWrapper<Board>()
                    .select(Board::getSort)
                    .orderByDesc(Board::getSort)
                    .last("LIMIT 1"));
            Integer lastSort = lastBoard == null ? null : lastBoard.getSort();
            sort = (lastSort == null ? 0 : lastSort + 1);
        }
        board.setSort(sort);
        this.save(board);

        // 如果创建时指定了版主
        if (req.moderatorId() != null && req.moderatorId() > 0) {
            appointModerator(req.moderatorId(), board.getId());
        }
    }

    @Override
    @Transactional
    public void updateBoard(Integer id, BoardRequest req) {
        Board board = this.getById(id);
        if (board == null) {
            throw new BusinessException("板块不存在");
        }
        board.setName(req.name());
        board.setDescription(req.description());
        if (req.sort() != null) {
            board.setSort(req.sort());
        }
        this.updateById(board);

        // 处理版主变更
        if (req.moderatorId() != null) {
            appointModerator(req.moderatorId(), id);
        }
    }

    private void appointModerator(Long userId, Integer boardId) {
        // 1. 先清理该板块原有的所有板主
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getManagedBoardId, boardId)
                .set(User::getManagedBoardId, null)
                .set(User::getRole, "USER"));

        // 2. 任命新板主
        if (userId > 0) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("要任命的用户不存在");
            }
            // 检查用户是否已经是管理员
            if ("ADMIN".equals(user.getRole())) {
                throw new BusinessException("不能将管理员任命为版主");
            }
            user.setRole("MODERATOR");
            user.setManagedBoardId(boardId);
            userMapper.updateById(user);
        }
    }

    @Override
    @Transactional
    public void deleteBoard(Integer id) {
        // 清理版主关系
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getManagedBoardId, id)
                .set(User::getManagedBoardId, null)
                .set(User::getRole, "USER"));
        this.removeById(id);
    }
}
