package com.huawei.ai_platform.rss.infrastructure.ai.executor;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiInvalidStateException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.enums.AiResultEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction2Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiDriverResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Common AI validation mechanics
 *
 * @author Borodulin Artem
 * @since 2026.04.29
 */
@Component("aiValidationExecutor")
@RequiredArgsConstructor
@Slf4j
public class AiValidationExecutor implements AiFunction2Executor<String, String, String> {
    private final AiExecutor aiExecutor;

    @Override
    public String runFunction(String inputParam_1, String inputParam_2, AiStageParameters aiStageParameters) {
        int countAttempts = 1;

        ClassPathResource systemPromptResource = new ClassPathResource(aiStageParameters.getSystemPrompt());
        ClassPathResource userPromptResource = new ClassPathResource(aiStageParameters.getUserPrompt());

        while (countAttempts <= aiStageParameters.getMaxAttempts()) {
            try (InputStream systemInputStream = systemPromptResource.getInputStream();
                 InputStream userInputStream = userPromptResource.getInputStream()) {

                String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);
                String userPromptContent = String.format(new String(userInputStream.readAllBytes(), StandardCharsets.UTF_8),
                        inputParam_1, inputParam_2
                );

                AiDriverResponse result = aiExecutor.performOperation(systemPromptContent, userPromptContent, aiStageParameters.getTemperature(),
                        aiStageParameters.getModel()
                );

                if (result == null) {
                    throw new AiNullResultException("Result from the AI is null");
                }

                if (result.getResultEnum() == AiResultEnum.FAILURE) {
                    throw new AiInvalidStateException("Result from AI is FAILURE");
                }

                return result.getText();
            } catch (Exception e) {
                log.warn("AI {} side: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}",
                        aiStageParameters.getStageName(),
                        countAttempts++, aiStageParameters.getMaxAttempts(), aiStageParameters.getId(),
                        e.getMessage()
                );
            }
        }

        throw new AiInvalidStateException(String.format("AI %s: count attempts has exceeded; ID = %s",
                aiStageParameters.getStageName(), aiStageParameters.getId()));
    }
}
