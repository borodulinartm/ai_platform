package com.huawei.ai_platform.rss.infrastructure.ai.model.event;

import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import com.huawei.ai_platform.rss.model.RssData;
import lombok.*;

/**
 * Wrapper class for storing cleaning start event
 *
 * @author Borodulin Artem
 * @since 2026.04.06
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CleaningCreatedEvent {
    private AiCleaningRequest aiCleaningRequest;
}
