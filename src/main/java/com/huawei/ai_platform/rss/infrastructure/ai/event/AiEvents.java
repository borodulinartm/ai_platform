package com.huawei.ai_platform.rss.infrastructure.ai.event;

import com.huawei.ai_platform.rss.application.service.RssTranslationService;
import com.huawei.ai_platform.rss.application.service.RssTranslationOrchestration;
import com.huawei.ai_platform.rss.infrastructure.ai.assembler.AiTranslationMapper;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.CleaningCreatedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.RelevanceCheckCompletedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.RelevanceCheckCreatedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationCompletedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationCreatedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.*;

/**
 * Consumer event class for some AI events
 *
 * @author Borodulin Artem
 * @since 2026.03.24
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiEvents {
    private final AiTranslationMapper aiTranslationMapper;
    private final RssTranslationService rssTranslationService;
    private final RssTranslationOrchestration rssTranslationOrchestration;
    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    public void onCreateRequestToTranslation(TranslationCreatedEvent translationCreatedEvent) {
        if (translationCreatedEvent != null) {
            rssTranslationService.insertNewArticleTranslations(List.of(translationCreatedEvent.getRecords()),
                    translationCreatedEvent.getStatusEnum());

            AiCleaningRequest cleaningRequests = aiTranslationMapper.convert(translationCreatedEvent.getRecords());
            applicationEventPublisher.publishEvent(new RelevanceCheckCreatedEvent(cleaningRequests));
            rssTranslationOrchestration.checkRelevance(cleaningRequests);
        } else {
            log.warn("For 'onCreateRequestToTranslation' produced empty data");
        }
    }

    @EventListener
    public void onRelevanceCheckCreated(RelevanceCheckCreatedEvent event) {
        if (event != null) {
            List<Long> idList = List.of(event.getAiCleaningRequest().getId());
            rssTranslationService.queryUpdateStatusByListData(idList, RELEVANCE_CHECK_PROCESSING);
        } else {
            log.warn("For 'onRelevanceCheckCreated' produced empty data");
        }
    }

    @EventListener
    public void onRelevanceCheckCompleted(RelevanceCheckCompletedEvent event) {
        if (event != null) {
            if (event.isPassed()) {
                rssTranslationOrchestration.cleanInputText(event.getAiCleaningRequest());
            } else {
                List<Long> idList = List.of(event.getAiCleaningRequest().getId());
                rssTranslationService.queryUpdateStatusByListData(idList, SKIPPED);
            }
        } else {
            log.warn("For 'onRelevanceCheckCompleted' produced empty data");
        }
    }

    @EventListener
    public void onCreateCleaningTranslation(CleaningCreatedEvent cleaningCreatedEvent) {
        if (cleaningCreatedEvent != null) {
            List<Long> idList = List.of(cleaningCreatedEvent.getAiCleaningRequest().getId());
            rssTranslationService.queryUpdateStatusByListData(idList, CLEANING_PROCESSING);
        } else {
            log.warn("For 'onCreateCleaningTranslation' produced empty data");
        }
    }

    @EventListener
    public void onStartTranslation(TranslationProcessingEvent translationProcessingEvent) {
        if (translationProcessingEvent != null) {
            List<Long> idList = List.of(translationProcessingEvent.getAiTranslationRequest().getArticleId());
            rssTranslationService.queryUpdateStatusByListData(idList, TRANSLATING_PROCESSING);

            rssTranslationOrchestration.translateInputData(translationProcessingEvent.getAiTranslationRequest());
        } else {
            log.warn("For 'onStartTranslation' produced empty data");
        }
    }

    @EventListener
    public void onFinishTranslation(TranslationCompletedEvent event) {
        if (event != null) {
            rssTranslationService.queryUpdateArticleTranslation(List.of(event.getResponses()), event.getStatusEnum(),
                    event.getReason());
        } else {
            log.warn("For 'onFinishTranslation' produced empty data");
        }
    }
}