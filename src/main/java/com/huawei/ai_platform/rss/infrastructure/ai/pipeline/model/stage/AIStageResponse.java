package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

/**
 * Response section for AI stage. Describes statuses and reason
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AIStageResponse {
    private boolean success;
    private String failureReason;

    public static AIStageResponse success() {
        return new AIStageResponse(true, StringUtils.EMPTY);
    }

    public static AIStageResponse failure(String reason) {
        return new AIStageResponse(false, reason);
    }
}
