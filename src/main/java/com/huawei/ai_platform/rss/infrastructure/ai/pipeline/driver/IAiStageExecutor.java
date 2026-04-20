package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AIStageResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import jakarta.annotation.Nonnull;

/**
 * AI stage executor
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@FunctionalInterface
public interface IAiStageExecutor {
    /**
     * Runs stage using stage parameters
     *
     * @param aiStageParameters AI stage parameters wrapper
     * @return AI stage response
     */
    AIStageResponse runStage(@Nonnull AiStageParameters aiStageParameters);
}
