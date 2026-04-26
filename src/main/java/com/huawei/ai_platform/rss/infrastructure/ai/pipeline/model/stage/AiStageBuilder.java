package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction2Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.function.Function;

/**
 * Stage builder
 *
 * @author Borodulin Artem
 * @since 2026.04.24
 */
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class AiStageBuilder {

    public static <A, O> AiStage with1Parameter(String stageName, AiTypedKey<A> input, AiTypedKey<O> outputs,
                                                AiStageParameters parameters, AiFunction1Executor<A, O> extractor) {
        Function<AiPipelineContext, AIStageResponse> decoratorFunction = aiPipelineContext -> {
            try {
                A inputParam = aiPipelineContext.getStageResult(input);
                O resultExecution = extractor.runFunction(inputParam, parameters);

                aiPipelineContext.addStageResult(outputs, resultExecution);
                return AIStageResponse.success();
            } catch (Exception exception) {
                log.error("An error has occurred during stage execution: {}", exception.getMessage());
                return AIStageResponse.failure(exception.getMessage());
            }
        };

        return new AiStageImpl(
                stageName, Set.of(input), Set.of(outputs), decoratorFunction
        );
    }

    public static <A, B, O> AiStage add2Parameter(String stageName, AiTypedKey<A> input1,
                                                  AiTypedKey<B> input2, AiTypedKey<O> resultKey,
                                                  AiStageParameters aiStageParameters,
                                                  AiFunction2Executor<A, B, O> function2Executor) {
        Function<AiPipelineContext, AIStageResponse> decoratorFunction = aiPipelineContext -> {
            try {
                A inputParam = aiPipelineContext.getStageResult(input1);
                B inputParam2 = aiPipelineContext.getStageResult(input2);
                O resultExecution = function2Executor.runFunction(inputParam, inputParam2, aiStageParameters);

                aiPipelineContext.addStageResult(resultKey, resultExecution);

                return AIStageResponse.success();
            } catch (Exception e) {
                log.error("An error has occurred during stage execution: {}", e.getMessage());
                return AIStageResponse.failure(e.getMessage());
            }
        };

        return new AiStageImpl(
                stageName, Set.of(input1, input2), Set.of(resultKey), decoratorFunction
        );
    }
}
