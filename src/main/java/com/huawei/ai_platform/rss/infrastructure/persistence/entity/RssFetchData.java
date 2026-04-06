package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import lombok.*;

/**
 * Result fetch data from the DB
 *
 * @author Borodulin Artem b60078502
 * @since 2026.03.07
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssFetchData {
    private long id;

    private ArticleTranslationStatusEnum translationStatusEnum;

    private String title;
    private String translationTitleEn;
    private String titleZh;

    private String author;

    private String content;
    private String contentZh;
    private String cleanedContentEn;

    private String link;
    private long date;
    private String tags;

    private String feedName;
    private String feedDescription;
    private int feedId;
    private String feedUrl;
    private int feedPriority;
    private String feedWebsite;

    private int categoryId;
    private String categoryName;
}
