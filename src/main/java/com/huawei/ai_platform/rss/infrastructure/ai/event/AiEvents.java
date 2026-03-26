package com.huawei.ai_platform.rss.infrastructure.ai.event;

import com.huawei.ai_platform.rss.application.service.RssTranslationService;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationCompletedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationProcessingEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.PROCESSING;

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
    private final RssTranslationService rssTranslationService;

    @EventListener
    public void onCreateRequestToTranslation(TranslationCreatedEvent translationCreatedEvent) {
        if (translationCreatedEvent != null) {
            rssTranslationService.insertNewArticleTranslations(translationCreatedEvent.getRecords(),
                    translationCreatedEvent.getStatusEnum());
        } else {
            log.warn("For 'onCreatedEvent' produced empty data");
        }
    }

    @EventListener
    public void onStartTranslation(TranslationProcessingEvent translationProcessingEvent) {
        if (translationProcessingEvent != null) {
            rssTranslationService.queryUpdateStatusByListData(translationProcessingEvent.getIdList(), PROCESSING);
        } else {
            log.warn("For 'onProcessingEvent' produced empty data");
        }
    }

    @EventListener
    public void onFinishTranslation(TranslationCompletedEvent event) {
        if (event != null) {
            rssTranslationService.queryUpdateArticleTranslation(event.getResponses(), event.getStatusEnum(),
                    event.getReason());
        } else {
            log.warn("For 'onCompleteTranslation' produced empty data");
        }
    }
}
