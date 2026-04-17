package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * Entity structure for the RSS
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("admin_category")
public class RssCategoryEntity {
    @TableId
    public int id;
    private String name;
    private int kind;

    private int error;

    private RssCategoryAttribute attributes;
}
