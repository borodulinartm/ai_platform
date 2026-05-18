package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.factory;

import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiValidationException;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.enums.AiResultEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.IAiStageValidation;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiResultText;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.*;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Unary stage factory class
 *
 * @author Borodulin Artem
 * @since 2026.05.01
 */
@Slf4j
public class AiUnaryStageFactory {

    /**
     * Creates stage for the pipeline
     * @param stageName stage name
     * @param input typed key (argument from previous stages)
     * @param outputs typed key (result value)
     * @param parameters wrapper of the different parameters
     * @param extractor executor function which performs different operations
     * @param validation AI stage validation
     * @param validationParameters validation parameters
     * @return AI stage contract
     * @param <A> Argument (input type)
     * @param <O> Argument (Output type)
     */
    public <A, O> AiStage<O> createStage(@Nonnull String stageName, @Nonnull AiTypedKey<A> input,
                                         @Nonnull AiTypedKey<O> outputs,
                                         @Nonnull AiStageParameters parameters,
                                         @Nonnull AiFunction1Executor<A, O> extractor,
                                         IAiStageValidation<A, O> validation,
                                         AiStageParameters validationParameters) {
        Function<AiPipelineContext, AIStageResponse<O>> decoratorFunction = aiPipelineContext -> {
            int countAttempts = 1;
            int maxAttemptsCount = parameters.getMaxAttempts();

            List<String> errorCollector = new ArrayList<>();

            while (countAttempts <= maxAttemptsCount) {
                try {
                    A inputParam = aiPipelineContext.getStageResult(input);
                    AiResultText<O> resultExecution = extractor.runFunction(inputParam, parameters);

                    // Here, perform some after business logic side. In that case, validation
                    // If data is not valid, then throw an exception and try again. Otherwise, return with success status
                    if (validation != null) {
                        AiStageValidationResult validationResult = validation.validateStage(
                                AiResultText.of(AiResultEnum.OK, inputParam), resultExecution, validationParameters
                        );
                        if (!validationResult.isSuccess()) {
                            throw new AiValidationException(validationResult.getFailureReason());
                        }
                    }

                    aiPipelineContext.addStageResult(outputs, resultExecution.getText());

                    return AIStageResponse.success(resultExecution.getText());
                } catch (Exception exception) {
                    errorCollector.add(exception.getMessage());

                    log.warn("AI {} side: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}",
                            parameters.getStageName(),
                            countAttempts++, parameters.getMaxAttempts(), parameters.getId(),
                            exception.getMessage()
                    );
                }
            }

            return AIStageResponse.failure(String.format("Stage - %s. ID = %s. Errors = %s. Limit attempts", parameters.getStageName(),
                    parameters.getId(), String.join(System.lineSeparator(), errorCollector))
            );
        };

        return new AiStageImpl<>(
                stageName, Set.of(input), Set.of(outputs), decoratorFunction, parameters, validation
        );
    }
}
