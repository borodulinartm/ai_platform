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
    public int id;
    private String url;
    private int category;
    private String name;
    private String website;
    private String description;
    private int priority;
}
