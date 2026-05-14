package com.huawei.ai_platform.rss.infrastructure.ai.executor;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiInvalidStateException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.enums.AiResultEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiDriverResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiResultText;
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
    public AiResultText<String> runFunction(String inputParam, AiStageParameters aiStageParameters) {
        ClassPathResource systemPromptResource = new ClassPathResource(aiStageParameters.getSystemPrompt());
        ClassPathResource userPromptResource = new ClassPathResource(aiStageParameters.getUserPrompt());

        try (InputStream systemInputStream = systemPromptResource.getInputStream();
             InputStream userInputStream = userPromptResource.getInputStream()) {

            String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);
            String userPromptContent = String.format(new String(userInputStream.readAllBytes(), StandardCharsets.UTF_8),
                    inputParam
            );

            AiDriverResponse result = aiExecutor.performOperation(systemPromptContent, userPromptContent, aiStageParameters.getTemperature(),
                    aiStageParameters.getModel()
            );
            if (result == null) {
                throw new AiNullResultException("Result from the AI is null");
            }

            if (result.getResultEnum() == AiResultEnum.FAILURE) {
                throw new AiInvalidStateException("Result from AI is failure");
            }

            int score = parseScore(result.getText());

            if (score == -1) {
                throw new AiInvalidStateException("Invalid score. ID = " + aiStageParameters.getId());
            }

            if (score <= threshold) {
                throw new AiInvalidStateException("score=" + score + ", threshold=" + threshold);
            }

            return AiResultText.of(result.getResultEnum(), result.getText());
        } catch (Exception e) {
            throw new AiInvalidStateException(e);
        }
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