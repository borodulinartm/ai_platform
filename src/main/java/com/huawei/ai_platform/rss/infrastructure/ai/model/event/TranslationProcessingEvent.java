package com.huawei.ai_platform.rss.infrastructure.ai.model.event;

import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import lombok.*;

import java.util.List;

/**
 * Storing class for the processing events
 *
 * @author Borodulin Artem
 * @since 2026.03.24
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TranslationProcessingEvent {
    private AiTranslationRequest aiTranslationRequest;
}
