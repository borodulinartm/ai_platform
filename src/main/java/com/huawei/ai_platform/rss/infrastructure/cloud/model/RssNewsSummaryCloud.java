package com.huawei.ai_platform.rss.infrastructure.cloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("articleTopSummaryEn")
    private String articleTopSummaryEn;
    
    @JsonProperty("articleTopSummaryZh")
    private String articleTopSummaryZh;

    @JsonProperty("categoryId")
    private int categoryId;
    
    @JsonProperty("articlesReport")
    private List<RssArticleSummaryCloud> articlesReport;
}
