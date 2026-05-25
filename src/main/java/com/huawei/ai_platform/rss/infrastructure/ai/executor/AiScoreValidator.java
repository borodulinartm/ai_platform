package com.huawei.ai_platform.rss.infrastructure.ai.executor;

import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiInvalidStateException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.enums.AiResultEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.IAiStageValidation;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiDriverResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiResultText;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageValidationResult;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Vibe validation tool for the AI validation side. It uses score mechanism in order to calculate result
 *
 * @author Borodulin Artem
 * @since 2026.05.14
 */
@Component("aiScoringValidator")
@RequiredArgsConstructor
@Slf4j
public class AiScoreValidator implements IAiStageValidation<String, String> {
    private final AiExecutor aiExecutor;

    @Override
    public @Nonnull AiStageValidationResult validateStage(@Nonnull AiResultText<String> inputData,
                                                          @Nonnull AiResultText<String> outputData, AiStageParameters parameters) {
        int countAttempts = 1;
        int maxAttemptsCount = parameters.getMaxAttempts();

        ClassPathResource systemPromptResource = new ClassPathResource(parameters.getSystemPrompt());
        ClassPathResource userPromptResource = new ClassPathResource(parameters.getUserPrompt());

        while (countAttempts <= maxAttemptsCount) {
            try (InputStream systemInputStream = systemPromptResource.getInputStream();
                 InputStream userInputStream = userPromptResource.getInputStream()) {

                String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);
                String userPromptContent = String.format(new String(userInputStream.readAllBytes(), StandardCharsets.UTF_8),
                        inputData.getText(), outputData.getText()
                );

                AiDriverResponse result = aiExecutor.performOperation(systemPromptContent, userPromptContent, parameters.getTemperature(),
                        parameters.getModel()
                );

                if (result == null) {
                    throw new AiNullResultException("Result from the AI is null");
                }

                if (result.getResultEnum() == AiResultEnum.FAILURE) {
                    throw new AiInvalidStateException("Result from AI is failure");
                }

                if (result.getResultEnum() == AiResultEnum.NO_CONTENT) {
                    return AiStageValidationResult.failure(
                            "Result from AI is empty string (no content). Not valid.",
                            inputData.getText(), outputData.getText()
                    );
                }

                // Predicate currently hardcoded. Maybe need to add basic abstract class with default predicate
                // and different implementations must extend basic class and perform operation of predicating
                int numericResult = Integer.parseInt(result.getText());
                if (numericResult >= 5) {
                    return AiStageValidationResult.success();
                }

                return AiStageValidationResult.failure(String.format("Score is invalid. Min - 5, current - %d", numericResult), StringUtils.EMPTY, StringUtils.EMPTY);
            } catch (IOException exception) {
                log.warn("AiDefaultValidator: exception for ID = {}. Attempt {}/{}. File = {}, Input text = {}, Output text = {}", parameters.getId(),
                        countAttempts++, parameters.getMaxAttempts(), parameters.getSystemPrompt(), inputData, outputData
                );
            }
        }

        return AiStageValidationResult.failure("Count validation attempts has exceeded", StringUtils.EMPTY, StringUtils.EMPTY);
    }
}
