package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiInvalidStateException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
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
public class RelevanceStageExecutor implements AiFunction1Executor<String, String> {
    private final AiExecutor aiExecutor;

    @Value("${ai.relevance.relevance-threshold:4}")
    private int threshold;

    @Override
    public String runFunction(String inputParam, AiStageParameters aiStageParameters) {
        int countAttempts = 1;

        ClassPathResource systemPromptResource = new ClassPathResource(aiStageParameters.getSystemPrompt());
        ClassPathResource userPromptResource = new ClassPathResource(aiStageParameters.getUserPrompt());

        while (countAttempts <= aiStageParameters.getMaxAttempts()) {
            try (InputStream systemInputStream = systemPromptResource.getInputStream();
                 InputStream userInputStream = userPromptResource.getInputStream()) {

                String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);
                String userPromptContent = String.format(new String(userInputStream.readAllBytes(), StandardCharsets.UTF_8),
                        inputParam
                );

                String result = aiExecutor.performOperation(systemPromptContent, userPromptContent, aiStageParameters.getTemperature(),
                        aiStageParameters.getModel()
                );
                if (result == null) {
                    throw new AiNullResultException("Result from the AI is null");
                }

                int score = parseScore(result.trim());

                if (score == -1) {
                    log.warn("Invalid score on attempt {}/{}: {}", countAttempts, aiStageParameters.getMaxAttempts(), result);

                    countAttempts++;
                    continue;
                }

                if (score <= threshold) {
                    throw new AiInvalidStateException("score=" + score + ", threshold=" + threshold);
                }

                return result;
            } catch (Exception e) {
                log.warn("Relevance check attempt {}/{} failed for ID={}: {}", countAttempts++, aiStageParameters.getMaxAttempts(),
                        aiStageParameters.getId(), e.getMessage()
                );
            }
        }

        throw new AiInvalidStateException(
                String.format("Relevance check failed after %s attempts for ID=%s, defaulting to pass", aiStageParameters.getMaxAttempts(), aiStageParameters.getId()));
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