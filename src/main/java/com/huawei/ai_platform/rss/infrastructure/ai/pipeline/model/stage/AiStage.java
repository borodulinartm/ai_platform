package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.IAiStageValidation;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;

import java.util.Set;
import java.util.function.Function;

/**
 * Implement an AI stage
 *
 * @author Borodulin Artem
 * @since 2026.04.24
 */
public interface AiStage<O> {
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
     * Contains AI stage parameters
     *
     * @return AI stage parameters
     */
    AiStageParameters getStageParameters();

    /**
     * Performs execution function
     *
     * @return Function: key -> pipeline context, value -> ai stage response
     */
    Function<AiPipelineContext, AIStageResponse<O>> executorFunction();

    /**
     * Returns validation side for the AI stage validation
     *
     * @return AI stage validation
     */
    IAiStageValidation<?, O> validation();
}
