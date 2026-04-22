package com.huawei.ai_platform.rss.infrastructure.ai.repo;

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

    @Value("${ai.relevance.countAttempts}")
    private int maxCountAttempts;

    @Value("${ai.relevance.temperature}")
    private Double temperature;

    public RelevanceCheckResult checkRelevance(RelevanceCheckRequest request) {
        String relevancePrompt = "prompt/relevance/relevance-check-prompt.txt";
        String userPrompt = "prompt/user-prompt.txt";

        String payload = String.format("Category: %s\nTitle: %s\nContent: %s",
                request.getCategoryName() != null ? request.getCategoryName() : "",
                request.getTitle() != null ? request.getTitle() : "",
                request.getContent() != null ? request.getContent() : "");

        AiPipelineRequest pipelineRequest = AiPipelineBuilder.createBuilder(PIPELINE_NAME)
                .addStage(
                        request.getId(), "RELEVANCE CHECK", relevanceStageExecutor,
                        relevancePrompt, userPrompt, payload,
                        "", temperature, maxCountAttempts
                ).build();

        AIPipelineResponse pipelineResponse = aiPipelineExecutor.executePipeline(pipelineRequest);
        int score = Integer.parseInt(pipelineResponse.getPayload().trim());
        return new RelevanceCheckResult(pipelineResponse.isSuccess(), score, pipelineResponse.getPayload());
    }

    public record RelevanceCheckResult(boolean passed, int score, String reason) {}
}