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
 * Vibe validation tool for the AI validation side
 *
 * @author Borodulin Artem
 * @since 2026.05.01
 */
@Component("aiDefaultValidator")
@RequiredArgsConstructor
@Slf4j
public class AiDefaultValidator implements IAiStageValidation<String, String> {
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
                        inputData.getText(), StringUtils.isBlank(outputData.getText()) ? "NO_CONTENT" : outputData.getText()
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
                    return AiStageValidationResult.success();
                }

                return AiStageValidationResult.failure(
                        String.format("Result from AI is not correct! Message = '%s'\nInput = %s\nOutput = %s\n", result.getText(),
                                inputData.getText(), outputData.getText())
                );
            } catch (IOException exception) {
                log.warn("AiDefaultValidator: exception for ID = {}. Attempt {}/{}. File = {}, Input text = {}, Output text = {}", parameters.getId(),
                        countAttempts++, parameters.getMaxAttempts(), parameters.getSystemPrompt(), inputData, outputData
                );
            }
        }

        return AiStageValidationResult.failure("Count attempts has exceeded");
    }
}
