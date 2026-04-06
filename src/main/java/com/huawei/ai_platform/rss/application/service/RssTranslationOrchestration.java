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
    OperationResult initTranslation(RssData inputList);
    void cleanInputText(AiCleaningRequest cleaningRequests);
    void translateInputData(AiTranslationRequest aiTranslationRequestList);
}
