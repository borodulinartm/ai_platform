package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiStageExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import lombok.*;

import java.util.Set;
import java.util.function.Function;

/**
 * Implement an AI stage
 *
 * @author Borodulin Artem
 * @since 2026.04.24
 */
public interface AiStage {
    /**
     * Extracts stage name
     *
     * @return stage name
     */
    String stageName();

    /**
     * Extracts set of an input
     *
     * @return set of an input
     */
    Set<AiTypedKey<?>> getInput();

    /**
     * Extracts of an outputs
     *
     * @return set of output name
     */
    Set<AiTypedKey<?>> getOutputs();

    /**
     * Performs execution function
     *
     * @return Function: key -> pipeline context, value -> ai stage response
     */
    Function<AiPipelineContext, AIStageResponse> executorFunction();
}
