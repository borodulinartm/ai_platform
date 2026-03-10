package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * Entity layer for the RSS
 *
 * @author Borodulin Artem b60078502
 * @since 2026.03.07
 */
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
    private Date date;
    private byte isRead;
}
