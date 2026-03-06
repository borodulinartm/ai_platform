package com.huawei.freshrss_news_upload.rss.model;

import lombok.*;

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
public class RssCategory {
    private int categoryId;
    private String categoryName;
}
