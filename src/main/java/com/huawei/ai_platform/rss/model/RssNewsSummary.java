package com.huawei.ai_platform.rss.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("categoryId")
    private int categoryId;

    @JsonProperty("articleTopSummaryEn")
    private String articleTopSummaryEn;
    
    @JsonProperty("articleTopSummaryZh")
    private String articleTopSummaryZh;

    @JsonProperty("articles")
    private List<RssArticleSummary> articles;
}
