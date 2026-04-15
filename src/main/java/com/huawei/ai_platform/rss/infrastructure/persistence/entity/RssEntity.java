package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("admin_entry")
public class RssEntity {
    @TableId
    private long id;

    private String title;
    private String author;
    private String content;
    private String link;
    private Long date;
    private byte isRead;

    @TableField("id_feed")
    private Integer feed;

    private List<RssAttributeValue> attributes;
}
