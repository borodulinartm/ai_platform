package com.huawei.ai_platform.rss.model;

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
public class RssArticleSummary {
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

    private String background;
    private String effects;
    private String eventSummary;
    private String technologyAndInnovation;
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
