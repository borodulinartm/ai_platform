package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import jakarta.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Model class for AI stage validation
 *
 * @author Borodulin Artem
 * @since 2025.05.01
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiStageValidationResult {
    private boolean success;
    private String failureReason;

    public static @Nonnull AiStageValidationResult success() {
        return new AiStageValidationResult(true, StringUtils.EMPTY);
    }

    public static @Nonnull AiStageValidationResult failure(@Nonnull String reason) {
        return new AiStageValidationResult(false, reason);
    }
}
