package com.huawei.ai_platform.rss.infrastructure.cloud.model;

import lombok.*;

/**
 * Cloud for the RSS feed
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssFeedCloud {
    public int feedId;
    private String url;
    private int categoryId;
    private String feedNameEn;
    private String feedNameZh;
    private String website;
    private String description;
    private int priority;
}
