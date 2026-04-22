package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import com.huawei.ai_platform.rss.model.RssData;

/**
 * Basic interface for the RSS orchestration side
 *
 * @author Borodulin Artem
 * @since 2026.04.06
 */
public interface RssTranslationOrchestration {
    /**
     * Inits translation
     *
     * @param inputList input
     * @return success if saga has started correctly, failed otherwise
     */
    OperationResult initTranslation(RssData inputList);

    /**
     * Performs cleaning text
     *
     * @param cleaningRequests clean request for the article
     */
    void cleanInputText(AiCleaningRequest cleaningRequests);

    /**
     * Performs relevance check
     *
     * @param cleaningRequests clean request for the article
     */
    void checkRelevance(AiCleaningRequest cleaningRequests);

    /**
     * Translated input data
     *
     * @param aiTranslationRequestList ai translation request
     */
    void translateInputData(AiTranslationRequest aiTranslationRequestList);
}
