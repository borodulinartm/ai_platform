package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import com.huawei.ai_platform.rss.model.RssData;
import jakarta.annotation.Nonnull;

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

    void queryUpdateArticleTranslation(List<AiTranslationResponse> responses,
                                       ArticleTranslationStatusEnum statusEnum);

    void queryUpdateStatusByListData(List<Long> idList, ArticleTranslationStatusEnum statusEnum);

    void insertNewArticleTranslations(List<RssData> rssDataList, ArticleTranslationStatusEnum statusEnum);
}
