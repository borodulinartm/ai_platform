package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageValidationResult;
import jakarta.annotation.Nonnull;

/**
 * AI stage validation
 *
 * @author Borodulin Artem
 * @since 2026.05.01
 */
public interface IAiStageValidation<I, O> {
    /**
     * Performs AI stage validation. For different stage it might be different validation implementations
     *
     * @param inputData input data from AI side
     * @param resultData result data that AI has processed
     * @param parameters AI parameters fpr the validation side
     * @return AI stage validation. Can be true/false
     */
    @Nonnull AiStageValidationResult validateStage(@Nonnull I inputData, @Nonnull O resultData, AiStageParameters parameters);
}
