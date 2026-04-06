package com.huawei.ai_platform.rss.application.repo;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
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
     * Extracts all untranslated news from datasource
     *
     * @return untranslated news
     */
    List<RssData> getNotTranslatedNews();

    /**
     * Performs updating article translation info
     *
     * @param responses  list of articles
     * @param statusEnum status
     * @param reason     reason
     */
    void queryUpdateArticleTranslation(List<AiTranslationResponse> responses,
                                       ArticleTranslationStatusEnum statusEnum, String reason);

    /**
     * Updates data by list of the statuses
     *
     * @param idList     list of id data
     * @param statusEnum which status do you want
     */
    void queryUpdateStatusByListData(List<Long> idList, ArticleTranslationStatusEnum statusEnum);

    /**
     * Creates new article translation
     *
     * @param rssDataList list of rcc data
     * @param statusEnum  status
     */
    void insertNewArticleTranslations(List<RssData> rssDataList, ArticleTranslationStatusEnum statusEnum);
}
