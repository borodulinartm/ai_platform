package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.executor;

import com.huawei.ai_platform.rss.infrastructure.ai.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.IAiStageExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AIStageResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RelevanceStageExecutor implements IAiStageExecutor {
    private final AiExecutor aiExecutor;

    @Value("${ai.relevance.relevance-threshold:4}")
    private int threshold;

    @Override
    public AIStageResponse runStage(@Nonnull AiStageParameters parameters) {
        int countAttempts = 1;

        ClassPathResource systemPromptResource = new ClassPathResource(parameters.getSystemPrompt());
        ClassPathResource userPromptResource = new ClassPathResource(parameters.getUserPrompt());

        while (countAttempts <= parameters.getMaxAttempts()) {
            try (InputStream systemInputStream = systemPromptResource.getInputStream();
                 InputStream userInputStream = userPromptResource.getInputStream()) {

                String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);
                String userPromptContent = String.format(new String(userInputStream.readAllBytes(), StandardCharsets.UTF_8),
                        parameters.getUserPayload()
                );

                String result = aiExecutor.performOperation(systemPromptContent, userPromptContent, parameters.getTemperature());
                if (result == null) {
                    throw new AiNullResultException("Result from the AI is null");
                }

                int score = parseScore(result.trim());

                if (score == -1) {
                    log.warn("Invalid score on attempt {}/{}: {}", countAttempts, parameters.getMaxAttempts(), result);
                    countAttempts++;
                    continue;
                }

                if (score <= threshold) {
                    return AIStageResponse.failure("score=" + score + ", threshold=" + threshold);
                }

                return AIStageResponse.success(result);
            } catch (Exception e) {
                log.warn("Relevance check attempt {}/{} failed for ID={}: {}", countAttempts++, parameters.getMaxAttempts(), parameters.getId(), e.getMessage());
            }
        }

        log.warn("Relevance check failed after {} attempts for ID={}, defaulting to pass", parameters.getMaxAttempts(), parameters.getId());
        return AIStageResponse.failure("failed after " + parameters.getMaxAttempts() + " attempts");
    }

    private int parseScore(String response) {
        String cleaned = response.trim();
        String numberOnly = cleaned.replaceAll("[^0-9]", "");

        if (numberOnly.isEmpty()) {
            return -1;
        }

        try {
            int score = Integer.parseInt(numberOnly);
            if (score < 1 || score > 10) {
                return -1;
            }
            return score;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}