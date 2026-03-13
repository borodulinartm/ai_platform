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
    private String categoryName;
    private int categoryId;
    private String articleTitle;
    private List<String> authors;
    private String articleLink;

    private String background;
    private String effects;
    private String eventSummary;
    private String technologyAndInnovation;
    private String valueAndImpact;
}
