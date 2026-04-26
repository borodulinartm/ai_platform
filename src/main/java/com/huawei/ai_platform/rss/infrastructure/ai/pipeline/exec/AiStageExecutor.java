package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AIStageResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;

/**
 * AI stage executor driver
 *
 * @author Borodulin Artem
 * @since 2026.04.24
 */
@FunctionalInterface
public interface AiStageExecutor {
    /**
     * Central method-decorator for executing each stages
     *
     * @param aiPipelineContext context
     * @param aiStageParameters stage parameters
     */
    AIStageResponse runStage(AiPipelineContext aiPipelineContext, AiStageParameters aiStageParameters);
}
