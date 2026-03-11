package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "admin_feed")
public class RssFeedEntity {
    @TableId
    public int id;
    private String url;
    private int kind;
    private int category;
    private String name;
    private String website;
    private String description;
    private int priority;
}
