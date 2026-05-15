package com.hd.forum.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象（返回给前端的用户数据结构）。
 *
 * 说明：
 * - 不包含 password 等敏感字段
 * - followCount / fansCount 为冗余统计字段，便于个人主页快速展示
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    private String role;
    private Integer status;
    private Integer managedBoardId;
    private Integer followCount;
    private Integer fansCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
