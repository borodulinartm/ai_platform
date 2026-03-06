package com.huawei.freshrss_news_upload.rss.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rss internal structure (aggregate from the persistence)
 *
 * @author Borodulin Artem
 * @since 2026.03.05
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssData {
    private long itemId;
    private String title;
    private List<String> authors;
    private String content;
    private String link;
    private List<String> tags;
    private LocalDateTime creationDate;
    private RssFeed feed;
    private RssCategory category;
}
