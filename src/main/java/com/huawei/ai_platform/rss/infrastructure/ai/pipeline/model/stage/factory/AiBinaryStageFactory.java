package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.factory;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction2Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AIStageResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageImpl;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.function.Function;

/**
 * AI binary stage factory class
 *
 * @author Borodulin Artem
 * @since 2026.05.01
 */
@Slf4j
public class AiBinaryStageFactory {

    public <A, B, O> AiStage<O> createStage(@Nonnull String stageName, @Nonnull AiTypedKey<A> input1,
                                            @Nonnull AiTypedKey<B> input2, @Nonnull AiTypedKey<O> resultKey,
                                            @Nonnull AiStageParameters aiStageParameters,
                                            @Nonnull AiFunction2Executor<A, B, O> function2Executor) {
        Function<AiPipelineContext, AIStageResponse<O>> decoratorFunction = aiPipelineContext -> {
            int countAttempts = 1;
            int maxAttemptsCount = aiStageParameters.getMaxAttempts();

            while (countAttempts <= maxAttemptsCount) {
                try {
                    A inputParam = aiPipelineContext.getStageResult(input1);
                    B inputParam2 = aiPipelineContext.getStageResult(input2);
                    O resultExecution = function2Executor.runFunction(inputParam, inputParam2, aiStageParameters);

                    aiPipelineContext.addStageResult(resultKey, resultExecution);

                    return AIStageResponse.success(resultExecution);
                } catch (Exception e) {
                    log.warn("AI {} side: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}",
                            aiStageParameters.getStageName(),
                            countAttempts++, aiStageParameters.getMaxAttempts(), aiStageParameters.getId(),
                            e.getMessage()
                    );
                }
            }

            return AIStageResponse.failure(String.format("AI %s side: Exceeded limit attempts. ID = %s", aiStageParameters.getStageName(),
                    aiStageParameters.getId())
            );
        };

        return new AiStageImpl<>(
                stageName, Set.of(input1, input2), Set.of(resultKey), decoratorFunction, aiStageParameters, null
        );
    }
}
