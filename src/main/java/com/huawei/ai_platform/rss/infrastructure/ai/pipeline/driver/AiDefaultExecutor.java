package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver;

import com.huawei.ai_platform.rss.infrastructure.ai.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiValidationException;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AIStageResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

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
public class AiDefaultExecutor implements IAiStageExecutor {
    private final AiExecutor aiExecutor;

    @Override
    public AIStageResponse runStage(@Nonnull AiStageParameters aiStageParameters,
                                    @Nonnull AiPipelineContext pipelineContext) {
        int countAttempts = 1;

        ClassPathResource systemPromptResource = new ClassPathResource(aiStageParameters.getSystemPrompt());
        ClassPathResource userPromptResource = new ClassPathResource(aiStageParameters.getUserPrompt());

        while (countAttempts <= aiStageParameters.getMaxAttempts()) {
            try (InputStream systemInputStream = systemPromptResource.getInputStream();
                 InputStream userInputStream = userPromptResource.getInputStream()) {

                String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);
                String userPromptContent = String.format(new String(userInputStream.readAllBytes(), StandardCharsets.UTF_8),
                        pipelineContext.getStageResult(pipelineContext.getPreviousStage())
                );

                String result = aiExecutor.performOperation(systemPromptContent, userPromptContent, aiStageParameters.getTemperature());
                if (result == null) {
                    throw new AiNullResultException("Result from the AI is null");
                }

                if (aiStageParameters.getValidationPredicate() != null &&
                       ! aiStageParameters.getValidationPredicate().test(pipelineContext.getStageResult(pipelineContext.getPreviousStage()), result)) {
                    throw new AiValidationException("Result is null");
                }

                return AIStageResponse.success(result.trim());
            } catch (Exception e) {
                log.warn("AI {} side: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}",
                        aiStageParameters.getName(),
                        countAttempts++, aiStageParameters.getMaxAttempts(), aiStageParameters.getId(),
                        e.getMessage()
                );
            }
        }

        return AIStageResponse.failure(String.format("AI %s: count attempts has exceeded; ID = %s",
                aiStageParameters.getName(), aiStageParameters.getId())
        );
    }
}
