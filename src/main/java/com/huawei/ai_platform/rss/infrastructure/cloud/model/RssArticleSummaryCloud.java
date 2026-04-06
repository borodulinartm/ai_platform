package com.huawei.ai_platform.rss.infrastructure.cloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssArticleSummaryCloud {
    
    @JsonProperty("articleTitleEn")
    private String articleTitleEn;
    
    @JsonProperty("articleAbstractEn")
    private String articleAbstractEn;
    
    @JsonProperty("articleLink")
    private String articleLink;
    
    @JsonProperty("authors")
    private List<String> authors;
    
    @JsonProperty("articleTitleZh")
    private String articleTitleZh;
    
    @JsonProperty("articleAbstractZh")
    private String articleAbstractZh;

    @JsonProperty("backgroundEn")
    private String backgroundEn;
    
    @JsonProperty("effectsEn")
    private String effectsEn;
    
    @JsonProperty("eventSummaryEn")
    private String eventSummaryEn;
    
    @JsonProperty("technologyAndInnovationEn")
    private String technologyAndInnovationEn;
    
    @JsonProperty("valueAndImpactEn")
    private String valueAndImpactEn;

    @JsonProperty("backgroundZh")
    private String backgroundZh;
    
    @JsonProperty("effectsZh")
    private String effectsZh;
    
    @JsonProperty("eventSummaryZh")
    private String eventSummaryZh;
    
    @JsonProperty("technologyAndInnovationZh")
    private String technologyAndInnovationZh;
    
    @JsonProperty("valueAndImpactZh")
    private String valueAndImpactZh;
}
