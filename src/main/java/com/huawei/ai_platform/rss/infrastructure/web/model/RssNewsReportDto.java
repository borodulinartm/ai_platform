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
public class RssNewsReportDto {
    private int categoryId;
    private String articleTitle;
    private String articleContent;
    private List<String> authors;
    private String articleLink;
}
