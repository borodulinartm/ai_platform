package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.IAiStageValidation;
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
    public @Nonnull AiStageValidationResult validateStage(@Nonnull String inputData, @Nonnull String outputData, AiStageParameters parameters) {
        int countAttempts = 1;
        int maxAttemptsCount = parameters.getMaxAttempts();

        ClassPathResource systemPromptResource = new ClassPathResource(parameters.getSystemPrompt());
        ClassPathResource userPromptResource = new ClassPathResource(parameters.getUserPrompt());

        while (countAttempts <= maxAttemptsCount) {
            try (InputStream systemInputStream = systemPromptResource.getInputStream();
                 InputStream userInputStream = userPromptResource.getInputStream()) {

                String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);
                String userPromptContent = String.format(new String(userInputStream.readAllBytes(), StandardCharsets.UTF_8),
                        inputData, outputData
                );

                String result = aiExecutor.performOperation(systemPromptContent, userPromptContent, parameters.getTemperature());
                if (result == null) {
                    throw new AiNullResultException("Result from the AI is null");
                }

                if (StringUtils.isBlank(result)) {
                    return AiStageValidationResult.success();
                }

                return AiStageValidationResult.failure(result);
            } catch (IOException exception) {
                log.warn("AiDefaultValidator: exception for ID = {}. Attempt {}/{}", parameters.getId(),
                        countAttempts++, parameters.getMaxAttempts()
                );
            }
        }

        return AiStageValidationResult.failure("Count attempts has exceeded");
    }
}
