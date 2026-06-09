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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Default AI executor
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@Component("defaultAiExecutor")
@RequiredArgsConstructor
@Slf4j
public class AiDefaultExecutor implements AiFunction1Executor<String, String> {
    private final AiExecutor aiExecutor;

    @Override
    public AiResultText<String> runFunction(String inputParam, AiStageParameters aiStageParameters) {
        ClassPathResource systemPromptResource = new ClassPathResource(aiStageParameters.getSystemPrompt());
        ClassPathResource userPromptResource = new ClassPathResource(aiStageParameters.getUserPrompt());

        try (InputStream systemInputStream = systemPromptResource.getInputStream();
             InputStream userInputStream = userPromptResource.getInputStream()) {

            String systemPromptContent = String.format(new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8),
                    aiStageParameters.getPreviousFailureMessage()
            );
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
                throw new AiInvalidStateException("Result is failure from the AI");
            }

            return AiResultText.of(result.getResultEnum(), result.getText());
        } catch (IOException exception) {
            throw new AiInvalidStateException(exception);
        }
    }
}
