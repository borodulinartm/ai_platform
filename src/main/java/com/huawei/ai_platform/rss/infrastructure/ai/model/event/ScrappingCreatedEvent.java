package com.huawei.ai_platform.rss.infrastructure.ai.model.event;

import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping.AiScrappingRequest;
import lombok.*;

/**
 * Wrapper class for storing scrapping start event
 *
 * @author Borodulin Artem
 * @since 2026.04.16
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScrappingCreatedEvent {
    private AiScrappingRequest aiScrappingRequest;
}
