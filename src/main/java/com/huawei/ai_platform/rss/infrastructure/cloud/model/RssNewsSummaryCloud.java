package com.huawei.ai_platform.rss.infrastructure.cloud.model;

import lombok.*;

import java.util.List;

/**
 *  RSS news summary for the cloud section
 *
 * @author Borodulin Artem
 * @since 2026.03.12
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssNewsSummaryCloud {
    private String articleTopSummaryEn;
    private String articleTopSummaryZh;

    private int categoryId;
    private List<RssArticleSummaryCloud> articlesReport;
}
