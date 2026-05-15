package com.hd.forum.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 板块视图对象
 */
@Data
@Schema(description = "板块视图对象")
public class BoardVO {

    @Schema(description = "板块ID")
    private Integer id;

    @Schema(description = "板块名称")
    private String name;

    @Schema(description = "板块描述")
    private String description;

    @Schema(description = "排序优先级")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "板主昵称")
    private String moderatorName;

    @Schema(description = "板主ID")
    private Long moderatorId;

    @Schema(description = "帖子数量")
    private Integer postCount;
}
