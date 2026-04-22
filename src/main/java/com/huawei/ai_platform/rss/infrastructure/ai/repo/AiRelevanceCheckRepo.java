package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.RelevanceCheckRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiPipelineExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.IAiStageExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Relevance check section for the AI
 *
 * @author Borodulin Artem
 * @since 2026.04.22
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiRelevanceCheckRepo {
    public static final String PIPELINE_NAME = "RELEVANCE_CHECK";

    private final IAiStageExecutor relevanceStageExecutor;
    private final AiPipelineExecutor aiPipelineExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.cleaning.countAttempts}")
    private int maxCountAttempts;

    @Value("${ai.cleaning.temperature}")
    private Double temperature;

    public boolean checkRelevance(RelevanceCheckRequest request) {
        String relevancePrompt = "prompt/cleaning/relevance-check-prompt.txt";
        String userPrompt = "prompt/user-prompt.txt";

        String payload;
        try {
            payload = objectMapper.writeValueAsString(new RelevancePayload(
                    request.getTitle() != null ? request.getTitle() : "",
                    request.getContent() != null ? request.getContent() : "",
                    request.getCategoryName() != null ? request.getCategoryName() : ""
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize relevance request", e);
            return false;
        }

        AiPipelineRequest pipelineRequest = AiPipelineBuilder.createBuilder(PIPELINE_NAME)
                .addStage(
                        request.getId(), "RELEVANCE CHECK", relevanceStageExecutor,
                        relevancePrompt, userPrompt, payload,
                        "", temperature, maxCountAttempts
                ).build();

        AIPipelineResponse pipelineResponse = aiPipelineExecutor.executePipeline(pipelineRequest);
        return pipelineResponse.isSuccess();
    }

    private record RelevancePayload(String title, String content, String categoryName) {}
}