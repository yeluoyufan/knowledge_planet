package com.hd.forum.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.forum.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hd.forum.vo.ConversationVO;
import com.hd.forum.vo.MessageVO;
import com.hd.forum.vo.UnreadCountVO;

import java.util.List;

/**
 * 站内消息业务服务接口。
 *
 * 说明：
 * - 私信与通知统一存储在消息表中，通过 type 区分（CHAT/COMMENT/SYSTEM）
 * - Service 层负责：写入消息、读取消息列表、维护已读状态、聚合未读统计等
 */
public interface IMessageService extends IService<Message> {

    /**
     * 发送私信（CHAT）。
     */
    void sendPrivateMessage(Long toUserId, String content);

    /**
     * 创建系统/评论通知。
     * 这是一个通用内部方法，通常由评论、点赞、审核等业务触发调用。
     */
    void createSystemNotification(Long fromId, Long toId, String type, String content);

    /**
     * 获取当前登录用户的消息列表（分页）。
     */
    Page<MessageVO> getMyMessages(Page<Message> page, String type);

    /**
     * 标记某条消息为已读。
     */
    void markAsRead(Long id);

    /**
     * 获取未读消息总数（用于前端小红点）。
     */
    Long getUnreadCount();

    /**
     * 获取会话列表（私信的左侧会话列表）。
     */
    List<ConversationVO> getConversationList();

    /**
     * 获取与某个用户的聊天记录（私信详情）。
     */
    List<MessageVO> getChatHistory(Long partnerId);

    /**
     * 批量标记已读。
     */
    void batchMarkAsRead(List<Long> ids);

    /**
     * 将当前用户的消息全部标记为已读。
     */
    void markAllAsRead();

    /**
     * 获取分类未读数（按类型聚合）。
     */
    UnreadCountVO getUnreadCountByType();
}
