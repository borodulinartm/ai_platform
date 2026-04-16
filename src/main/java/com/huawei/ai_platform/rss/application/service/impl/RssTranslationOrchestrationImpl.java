package com.huawei.ai_platform.rss.application.service.impl;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.service.RssLinkExtractor;
import com.huawei.ai_platform.rss.application.service.RssScrappingValidation;
import com.huawei.ai_platform.rss.application.service.RssTranslationOrchestration;
import com.huawei.ai_platform.rss.application.service.impl.link.LinkExtractorFactory;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.*;
import com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping.AiScrappingRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping.AiScrappingResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.repo.AiCleaningArticlesRepo;
import com.huawei.ai_platform.rss.infrastructure.ai.repo.AiScrappingRepo;
import com.huawei.ai_platform.rss.infrastructure.ai.repo.AiTranslatorRepo;
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
    private final AiScrappingRepo aiScrappingRepo;
    private final RssScrappingValidation rssScrappingValidation;
    private final LinkExtractorFactory linkExtractorFactory;

    @Override
    public OperationResult initTranslation(RssData inputList) {
        log.info("STAGE 1 vs 4: accepting an ID = {}", inputList.getArticleId());
        applicationEventPublisher.publishEvent(new TranslationCreatedEvent(inputList, INIT));

        return OperationResult.builder().state(OperationResultEnum.SUCCESS).reason("").build();
    }

    @Override
    public void scrapContent(AiScrappingRequest aiScrappingRequest) {
        log.info("STAGE 2 vs 4: scrapping an article for ID = {}", aiScrappingRequest.getId());
        applicationEventPublisher.publishEvent(new ScrappingCreatedEvent(aiScrappingRequest));

        if (rssScrappingValidation.needScrap(aiScrappingRequest)) {
            RssLinkExtractor rssLinkExtractor = linkExtractorFactory.getByLink(aiScrappingRequest.getArticleLink());

            String normalLink = rssLinkExtractor.getUrl(aiScrappingRequest.getArticleLink());
            if (normalLink == null) {
                AiTranslationResponse translationResponse = AiTranslationResponse.failureResponse(aiScrappingRequest.getId(),
                        "The result link is null. Cannot proceed operation. Returning for that ID"
                );
                applicationEventPublisher.publishEvent(new TranslationCompletedEvent(translationResponse, FAILURE, translationResponse.getReason()));

                return;
            }

            aiScrappingRequest.setArticleLink(normalLink);

            AiScrappingResponse aiScrappingResponse = aiScrappingRepo.runScrapping(aiScrappingRequest);
            if (aiScrappingResponse.isSuccess()) {
                AiCleaningRequest aiCleaningRequest = new AiCleaningRequest(
                        aiScrappingResponse.getId(), aiScrappingResponse.getArticleTitle(),
                        aiScrappingResponse.getArticleContentScrapped(), aiScrappingResponse.getArticleLink(),
                        aiScrappingResponse.getRssAttributeValue()
                );

                applicationEventPublisher.publishEvent(new CleaningCreatedEvent(aiCleaningRequest));
            } else {
                AiTranslationResponse translationResponse = AiTranslationResponse.failureResponse(aiScrappingResponse.getId(),
                        aiScrappingResponse.getReason()
                );
                applicationEventPublisher.publishEvent(new TranslationCompletedEvent(translationResponse, FAILURE, aiScrappingResponse.getReason()));

                log.error("STAGE 2 vs 4: Translation for ID = {} has completed with failure :(", aiScrappingResponse.getId());
            }
        } else {
            AiCleaningRequest aiCleaningRequest = new AiCleaningRequest(
                    aiScrappingRequest.getId(), aiScrappingRequest.getArticleTitle(),
                    aiScrappingRequest.getArticleContent(), aiScrappingRequest.getArticleLink(),
                    aiScrappingRequest.getAttributes()
            );

            applicationEventPublisher.publishEvent(new CleaningCreatedEvent(aiCleaningRequest));
        }
    }

    @Override
    public void cleanInputText(AiCleaningRequest cleaningRequests) {
        log.info("STAGE 3 vs 4: running cleaning for the ID = {}", cleaningRequests.getId());

        AiCleaningResponse response = aiCleaningArticlesRepo.processCleaning(cleaningRequests);
        if (response.isSuccess()) {
            AiTranslationRequest aiTranslationRequest = new AiTranslationRequest(response.getId(), response.getArticleTitleCleaned(),
                    response.getArticleContentCleaned(), response.getArticleLink()
            );

            applicationEventPublisher.publishEvent(new TranslationProcessingEvent(aiTranslationRequest));
        } else {
            AiTranslationResponse translationResponse = AiTranslationResponse.failureResponse(response.getId(), response.getReason());
            applicationEventPublisher.publishEvent(new TranslationCompletedEvent(translationResponse, FAILURE, response.getReason()));

            log.error("STAGE 3 vs 4: Translation for ID = {} has completed with failure :(", cleaningRequests.getId());
        }
    }

    @Override
    public void translateInputData(AiTranslationRequest aiTranslationRequestList) {
        log.info("STAGE 4 vs 4: running translating for the ID = {}", aiTranslationRequestList.getArticleId());

        AiTranslationResponse response = aiTranslatorRepo.translate(aiTranslationRequestList);
        if (response.isSuccess()) {
            applicationEventPublisher.publishEvent(new TranslationCompletedEvent(response, FINISH, "Success"));
            log.info("STAGE 4 vs 4: translation for ID = {} has completed successfully", aiTranslationRequestList.getArticleId());
        } else {
            AiTranslationResponse translationResponse = AiTranslationResponse.failureResponse(response.getArticleId(), response.getReason());
            applicationEventPublisher.publishEvent(new TranslationCompletedEvent(translationResponse, FAILURE, response.getReason()));

            log.error("STAGE 4 vs 4: translation for ID = {} has completed with failure :(", aiTranslationRequestList.getArticleId());
        }
    }
}
