package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import lombok.*;

/**
 * Entity for storing article translations
 *
 * @author Borodulin Artem
 * @since 2026.03.24
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "article_translations")
public class RssArticleTranslationEntity {
    @TableId
    private Long id;

    private Long articleCreateDate;
    private String titleEn;
    private String titleZh;
    private String contentZh;
    private String contentEn;
    private ArticleTranslationStatusEnum status;
}
