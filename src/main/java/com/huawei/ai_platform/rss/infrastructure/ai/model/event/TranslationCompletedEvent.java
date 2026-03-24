package com.huawei.ai_platform.rss.infrastructure.ai.model.event;

import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import lombok.*;

import java.util.List;

/**
 * Storing class for the completed events. Completion can be with failure or success
 *
 * @author Borodulin Artem
 * @since 2026.03.24
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TranslationCompletedEvent {
    private List<AiTranslationResponse> responses;
    private ArticleTranslationStatusEnum statusEnum;
}
