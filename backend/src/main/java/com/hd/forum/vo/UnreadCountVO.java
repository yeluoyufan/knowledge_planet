package com.hd.forum.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 未读消息统计的返回对象。
 *
 * 用于前端消息中心的小红点/分类未读数展示。
 */
@Data
@Builder
public class UnreadCountVO {
    private Long total;

    private Long chat;

    private Long comment;

    private Long system;

    private Long like;

    private Long favorite;

    private Long audit;

    private Long top;
}
