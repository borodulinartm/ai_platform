package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import com.huawei.ai_platform.rss.model.RssData;

import java.util.List;

/**
 * Input port for the translation
 *
 * @author Borodulin Artem
 * @since 2026.03.20
 */
public interface RssTranslationService {
    /**
     * Enables translation
     *
     * @return OperationResult: success/failure
     */
    OperationResult syncTranslation();

    /**
     * Updates article translation
     *
     * @param responses  list of data
     * @param statusEnum status of the translation
     * @param reason     some helpful text
     */
    void queryUpdateArticleTranslation(List<AiTranslationResponse> responses,
                                       ArticleTranslationStatusEnum statusEnum, String reason);

    /**
     * Updates status for group of the records
     *
     * @param idList     list of ID articles
     * @param statusEnum which status do you want to set
     */
    void queryUpdateStatusByListData(List<Long> idList, ArticleTranslationStatusEnum statusEnum);

    /**
     * Performs inserting translations
     *
     * @param rssDataList list of the rss data
     * @param statusEnum  status enumeration
     */
    void insertNewArticleTranslations(List<RssData> rssDataList, ArticleTranslationStatusEnum statusEnum);
}
