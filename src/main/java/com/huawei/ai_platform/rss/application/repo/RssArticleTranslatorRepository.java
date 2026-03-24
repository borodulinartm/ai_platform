package com.huawei.ai_platform.rss.application.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import com.huawei.ai_platform.rss.model.RssData;

import java.util.List;

/**
 * Interface which provides methods for translating
 *
 * @author Borodulin Artem
 * @since 2026.03.20
 */
public interface RssArticleTranslatorRepository {
    /**
     * Performs translating news
     *
     * @param compacts list of untranslated news
     * @return list of translated news
     */
    List<RssData> syncTranslation(List<RssData> compacts);

    /**
     * Extracts all untranslated news from datasource
     *
     * @return untranslated news
     */
    List<RssData> getNotTranslatedNews();

    void queryUpdateArticleTranslation(List<AiTranslationResponse> responses,
                                       ArticleTranslationStatusEnum statusEnum);

    void queryUpdateStatusByListData(List<Long> idList, ArticleTranslationStatusEnum statusEnum);

    void insertNewArticleTranslations(List<RssData> rssDataList, ArticleTranslationStatusEnum statusEnum);
}
