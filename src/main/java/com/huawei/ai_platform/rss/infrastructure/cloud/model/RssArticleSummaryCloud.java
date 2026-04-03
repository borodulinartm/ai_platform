package com.huawei.ai_platform.rss.infrastructure.cloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("authors")
    private List<String> authors;
    
    @JsonProperty("articleLink")
    private String articleLink;

    @JsonProperty("title")
    private String title;
    
    @JsonProperty("abstract")
    private String articleAbstract;
    
    @JsonProperty("titleCn")
    private String titleCn;
    
    @JsonProperty("abstractCn")
    private String abstractCn;

    @JsonProperty("background")
    private String background;
    
    @JsonProperty("effects")
    private String effects;
    
    @JsonProperty("eventSummary")
    private String eventSummary;
    
    @JsonProperty("technologyAndInnovation")
    private String technologyAndInnovation;
    
    @JsonProperty("valueAndImpact")
    private String valueAndImpact;

    @JsonProperty("backgroundCn")
    private String backgroundCn;
    
    @JsonProperty("effectsCn")
    private String effectsCn;
    
    @JsonProperty("eventSummaryCn")
    private String eventSummaryCn;
    
    @JsonProperty("technologyAndInnovationCn")
    private String technologyAndInnovationCn;
    
    @JsonProperty("valueAndImpactCn")
    private String valueAndImpactCn;
}
