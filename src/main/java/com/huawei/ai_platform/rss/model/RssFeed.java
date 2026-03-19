package com.huawei.ai_platform.rss.model;

import lombok.*;

import java.io.Serializable;

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
public class RssFeed implements Serializable {
    private int feedId;
    private String feedNameEn;
    private String feedNameZh;
    private String description;
    private int priority;
    private String url;
    private String website;
}
