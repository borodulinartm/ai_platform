package com.huawei.ai_platform.rss.model;

import com.huawei.ai_platform.rss.enums.RssTypeInfoEnum;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import lombok.*;

import java.io.Serializable;
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
public class RssData implements Serializable {
    private long articleId;

    private String articleTitleEn;
    private String articleTitleZh;

    private List<String> articleAuthors;

    private String articleContent;
    private String articleContentZh;

    private String articleLink;
    private List<String> articleTags;
    private LocalDateTime createDate;
    private RssFeed feed;
    private RssCategory rssCategory;
    private RssTypeInfoEnum typeInfoEnum;

    private ArticleTranslationStatusEnum translationStatusEnum;

    /**
     * Inversion method for simplicity
     *
     * @return true if article is not prepared to translation, false otherwise
     */
    public boolean isNotTranslationExists() {
        if (translationStatusEnum == null) {
            return true;
        }

        return !translationStatusEnum.isTranslated();
    }
}
