package com.huawei.ai_platform.rss.infrastructure.ai.model.event;

import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import lombok.*;

/**
 * Wrapper class for relevance check completed event
 *
 * @author Borodulin Artem
 * @since 2026.04.22
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RelevanceCheckCompletedEvent {
    private AiCleaningRequest aiCleaningRequest;
    private boolean passed;
    private String reason;
}