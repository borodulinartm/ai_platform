package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import lombok.*;

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
    private String payload;
    private String failureReason;

    public static AIStageResponse success(String payload) {
        return new AIStageResponse(true, payload, "");
    }

    public static AIStageResponse failure(String reason) {
        return new AIStageResponse(false, "", reason);
    }
}
