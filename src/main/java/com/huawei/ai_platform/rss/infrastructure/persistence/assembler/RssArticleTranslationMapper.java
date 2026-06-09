package com.huawei.ai_platform.rss.infrastructure.persistence.assembler;

import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssArticleTranslationEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import com.huawei.ai_platform.rss.model.RssData;
import org.mapstruct.*;

/**
 * Mapper class for translation news
 *
 * @author Borodulin Artem
 * @since 2026.03.24
 */
@Mapper(componentModel = "spring")
public abstract class RssArticleTranslationMapper {
    /**
     * Performs converting data from aggregate to translation entity
     *
     * @param rssData    aggregate
     * @param statusEnum status of the translation article
     * @return entity
     */
    @Mapping(target = "articleCreateDate", source = "rssData.articleId")
    @Mapping(target = "titleZh", source = "rssData.articleTitleZh")
    @Mapping(target = "titleEn", source = "rssData.articleTitleEn")
    @Mapping(target = "contentZh", source = "rssData.articleContentZh")
    @Mapping(target = "contentEn", source = "rssData.articleContent")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "status", source = "statusEnum")
    public abstract RssArticleTranslationEntity convert(RssData rssData, String reason, ArticleTranslationStatusEnum statusEnum);

    /**
     * Performs converting data from aggregate to translation entity
     *
     * @param rssData    aggregate
     * @param statusEnum status of the translation article
     * @return entity
     */
    @Mapping(target = "articleCreateDate", source = "rssData.articleId")
    @Mapping(target = "titleZh", source = "rssData.articleTitleZh")
    @Mapping(target = "titleEn", source = "rssData.articleTitleEn")
    @Mapping(target = "contentZh", source = "rssData.articleContentZh")
    @Mapping(target = "contentEn", source = "rssData.articleContent")
    @Mapping(target = "status", source = "statusEnum")
    public abstract RssArticleTranslationEntity convert(RssData rssData, ArticleTranslationStatusEnum statusEnum);
}
