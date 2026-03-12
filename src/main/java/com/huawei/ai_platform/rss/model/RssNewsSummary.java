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
    private String categoryName;
    private int categoryId;
    private String articleTitle;
    private String articleContent;
    private List<String> authors;
    private String articleLink;
}
