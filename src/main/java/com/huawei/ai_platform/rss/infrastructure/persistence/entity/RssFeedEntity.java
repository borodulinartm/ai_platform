package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * Entity side for storing feed information
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "admin_feed")
public class RssFeedEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String url;
    private int kind;

    @TableField("category")
    private int categoryId;

    private String name;
    private String website;
    private String description;
    private int priority;
}
