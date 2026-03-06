package com.huawei.freshrss_news_upload.rss.model;

import lombok.*;

/**
 * Rss Feed internal structure
 *
 * @author Borodulin Artem
 * @since 2026.03.05
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssFeed {
    private int feedId;
    private String name;
    private String description;
    private int priority;
    private String url;
    private String website;
}
