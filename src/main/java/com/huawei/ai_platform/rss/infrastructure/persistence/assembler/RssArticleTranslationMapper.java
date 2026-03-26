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
    @Mapping(target = "articleCreateDate", source = "articleId")
    @Mapping(target = "titleZh", source = "articleTitleZh")
    @Mapping(target = "contentZh", source = "articleContentZh")
    @Mapping(target = "contentEn", source = "articleContent")
    public abstract RssArticleTranslationEntity convert(RssData rssData, @Context ArticleTranslationStatusEnum statusEnum);

    /**
     * Extra method - article translation
     *
     * @param entity     entity
     * @param statusEnum status
     */
    @AfterMapping
    public void afterMapping(@MappingTarget RssArticleTranslationEntity entity,
                             @Context ArticleTranslationStatusEnum statusEnum) {
        entity.setStatus(statusEnum);
    }
}
