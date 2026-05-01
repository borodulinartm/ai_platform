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
public class AIStageResponse<T> {
    private boolean success;
    private T content;
    private String failureReason;

    public static <T> AIStageResponse<T> success(T content) {
        return new AIStageResponse<T>(true, content, StringUtils.EMPTY);
    }

    public static <T> AIStageResponse<T> failure(String reason) {
        return new AIStageResponse<T>(false, null, reason);
    }
}
