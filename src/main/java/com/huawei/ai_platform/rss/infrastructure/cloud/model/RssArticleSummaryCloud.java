package com.huawei.ai_platform.rss.infrastructure.cloud.model;

import lombok.*;

import java.util.List;

/**
 * Aggregate structure - RSS input an articles from the web side
 *
 * @author Borodulin Artem
 * @since 2026.03.18
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssArticleSummaryCloud {
    private String articleTitle;
    private List<String> authors;
    private String articleLink;
    private String articleAbstract;

    private String background;
    private String effects;
    private String eventSummary;
    private String technologyAndInnovation;
    private String valueAndImpact;
}
