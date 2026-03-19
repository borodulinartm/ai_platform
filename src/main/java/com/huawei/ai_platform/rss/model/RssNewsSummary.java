package com.huawei.ai_platform.rss.model;

import lombok.*;

import java.util.List;

/**
 * Aggregate for the RSS news summary
 *
 * @author Borodulin Artem
 * @since 2026.03.12
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssNewsSummary {
    private int categoryId;

    private String articleTopSummaryEn;
    private String articleTopSummaryZh;

    private List<RssArticleSummary> articlesReport;
}
