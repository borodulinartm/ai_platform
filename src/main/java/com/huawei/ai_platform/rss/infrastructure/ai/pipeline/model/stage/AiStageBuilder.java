package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiValidationException;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction2Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.IAiStageValidation;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import jakarta.annotation.Nonnull;
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

    public static <A, O> AiStage<O> with1Parameter(@Nonnull String stageName, @Nonnull AiTypedKey<A> input,
                                                   @Nonnull AiTypedKey<O> outputs,
                                                   @Nonnull AiStageParameters parameters,
                                                   @Nonnull AiFunction1Executor<A, O> extractor,
                                                   IAiStageValidation<A, O> validation,
                                                   AiStageParameters validationParameters) {
        Function<AiPipelineContext, AIStageResponse<O>> decoratorFunction = aiPipelineContext -> {
            int countAttempts = 1;
            int maxAttemptsCount = parameters.getMaxAttempts();

            while (countAttempts <= maxAttemptsCount) {
                try {
                    A inputParam = aiPipelineContext.getStageResult(input);
                    O resultExecution = extractor.runFunction(inputParam, parameters);

                    // Here, perform some after business logic side. In that case, validation
                    // If data is not valid, then throw an exception and try again. Otherwise, return with success status
                    if (validation != null) {
                        AiStageValidationResult validationResult = validation.validateStage(inputParam, resultExecution,
                                validationParameters
                        );
                        if (!validationResult.isSuccess()) {
                            throw new AiValidationException(validationResult.getFailureReason());
                        }
                    }

                    aiPipelineContext.addStageResult(outputs, resultExecution);

                    return AIStageResponse.success(resultExecution);
                } catch (Exception exception) {
                    log.warn("AI {} side: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}",
                            parameters.getStageName(),
                            countAttempts++, parameters.getMaxAttempts(), parameters.getId(),
                            exception.getMessage()
                    );
                }
            }

            return AIStageResponse.failure(String.format("AI %s side: Exceeded limit attempts. ID = %s", parameters.getStageName(),
                    parameters.getId())
            );
        };

        return new AiStageImpl<>(
                stageName, Set.of(input), Set.of(outputs), decoratorFunction, parameters, validation
        );
    }

    // For 2-parameter side validation disabled
    public static <A, B, O> AiStage<O> with2Parameter(@Nonnull String stageName, @Nonnull AiTypedKey<A> input1,
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
