package com.huawei.ai_platform.rss.application.service.impl;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.service.RssTranslationOrchestration;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.CleaningCreatedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationCompletedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationCreatedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationProcessingEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.repo.cleaning.AiCleaningArticlesRepo;
import com.huawei.ai_platform.rss.infrastructure.ai.repo.translation.AiTranslatorRepo;
import com.huawei.ai_platform.rss.model.RssData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.*;

/**
 * Implementation for the RSS orchestration translation side
 *
 * @author Borodulin Artem
 * @since 2026.04.06
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RssTranslationOrchestrationImpl implements RssTranslationOrchestration {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AiCleaningArticlesRepo aiCleaningArticlesRepo;
    private final AiTranslatorRepo aiTranslatorRepo;

    @Override
    public OperationResult initTranslation(RssData inputList) {
        log.info("STAGE 1 vs 3: accepting an ID = {}", inputList.getArticleId());
        applicationEventPublisher.publishEvent(new TranslationCreatedEvent(inputList, INIT));

        return OperationResult.builder().state(OperationResultEnum.SUCCESS).reason("").build();
    }

    @Override
    public void cleanInputText(AiCleaningRequest cleaningRequests) {
        log.info("STAGE 2 vs 3: running cleaning for the ID = {}", cleaningRequests.getId());
        applicationEventPublisher.publishEvent(new CleaningCreatedEvent(cleaningRequests));

        AiCleaningResponse response = aiCleaningArticlesRepo.processCleaning(cleaningRequests);
        if (response.isSuccess()) {
            AiTranslationRequest aiTranslationRequest = new AiTranslationRequest(response.getId(), response.getArticleTitleCleaned(),
                    response.getArticleContentCleaned(), response.getArticleLink()
            );

            applicationEventPublisher.publishEvent(new TranslationProcessingEvent(aiTranslationRequest));
        } else {
            AiTranslationResponse translationResponse = AiTranslationResponse.failureResponse(response.getId(), response.getReason());
            applicationEventPublisher.publishEvent(new TranslationCompletedEvent(translationResponse, FAILURE, translationResponse.getReason()));

            log.error("STAGE 2 vs 3: Translation for ID = {} has completed with failure :(", cleaningRequests.getId());
        }
    }

    @Override
    public void translateInputData(AiTranslationRequest aiTranslationRequestList) {
        log.info("STAGE 3 vs 3: running translating for the ID = {}", aiTranslationRequestList.getArticleId());

        AiTranslationResponse response = aiTranslatorRepo.translate(aiTranslationRequestList);
        if (response.isSuccess()) {
            applicationEventPublisher.publishEvent(new TranslationCompletedEvent(response, FINISH, "Success"));
            log.info("STAGE 3 vs 3: translation for ID = {} has completed successfully", aiTranslationRequestList.getArticleId());
        } else {
            AiTranslationResponse translationResponse = AiTranslationResponse.failureResponse(response.getArticleId(), response.getReason());
            applicationEventPublisher.publishEvent(new TranslationCompletedEvent(translationResponse, FAILURE, translationResponse.getReason()));

            log.error("STAGE 3 vs 3: translation for ID = {} has completed with failure :(", aiTranslationRequestList.getArticleId());
        }
    }
}
