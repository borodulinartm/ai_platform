package com.huawei.ai_platform.rss.infrastructure.ai.repo.validation;

import com.huawei.ai_platform.rss.infrastructure.ai.model.validation.ValidationResponse;
import jakarta.annotation.Nonnull;

/**
 * AI validation status
 *
 * @author Borodulin Artem
 * @since 2026.04.15
 */
public interface IAiValidation<T> {
    /**
     * Validates provided data
     *
     * @param request request
     * @return ValidationResponse with status: success/failure
     */
    @Nonnull
    ValidationResponse validate(@Nonnull T request);
}
