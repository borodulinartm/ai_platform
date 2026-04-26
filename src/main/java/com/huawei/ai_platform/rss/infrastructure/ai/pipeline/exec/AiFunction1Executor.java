package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;

/**
 * Interface for executing different business logics
 *
 * @param <A> input type
 * @param <R> return type
 * @author Borodulin Artem
 * @since 2026.04.24
 */
public interface AiFunction1Executor<A, R> {
    /**
     * Performs function executing
     *
     * @param inputParam        input parameter
     * @param aiStageParameters stage parameters (model, temperature, etc.)
     * @return some response different type
     */
    R runFunction(A inputParam, AiStageParameters aiStageParameters);
}
