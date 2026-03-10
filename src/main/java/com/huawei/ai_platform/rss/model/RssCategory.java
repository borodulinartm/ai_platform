package com.huawei.ai_platform.rss.model;

import lombok.*;

import java.io.Serializable;

/**
 * Rss category data
 *
 * @author Borodulin Artem
 * @since 2026.03.05
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssCategory implements Serializable {
    private int categoryId;
    private String categoryName;
}
