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
    private List<String> authors;
    private String articleLink;

    private String articleTitleEn;
    private String articleAbstractEn;
    private String backgroundEn;
    private String effectsEn;
    private String eventSummaryEn;
    private String technologyAndInnovationEn;
    private String valueAndImpactEn;

    private String articleTitleZh;
    private String articleAbstractZh;
    private String backgroundZh;
    private String effectsZh;
    private String eventSummaryZh;
    private String technologyAndInnovationZh;
    private String valueAndImpactZh;
}
