package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;

/**
 * Interface for executing different business logics (2 parameters)
 *
 * @param <A> input type
 * @param <R> return type
 * @author Borodulin Artem
 * @since 2026.04.24
 */
public interface AiFunction2Executor<A, B, R> {
    /**
     * Performs function executing
     *
     * @param inputParam_1      input parameter 1
     * @param inputParam_2      input parameter 2
     * @param aiStageParameters stage parameters
     * @return some response different type
     */
    R runFunction(A inputParam_1, B inputParam_2, AiStageParameters aiStageParameters);
}
