package com.huawei.ai_platform.rss.infrastructure.web.model;

import lombok.*;

import java.util.List;

/**
 * News report gotten from the web side
 *
 * @author Borodulin Artem
 * @since 2026.03.12
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssReportDto {
    private int categoryId;
    private List<RssArticlesReportDto> articlesReport;
    private String articleTopSummary;
}
