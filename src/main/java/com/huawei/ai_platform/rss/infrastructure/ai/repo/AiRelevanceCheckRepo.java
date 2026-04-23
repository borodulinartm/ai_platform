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
        String score = pipelineResponse.isSuccess()
                ? pipelineResponse.getPayload().trim()
                : extractScoreFromReason(pipelineResponse.getFailureReason());
        String reason = pipelineResponse.isSuccess() ? score : pipelineResponse.getFailureReason();
        return new RelevanceCheckResult(pipelineResponse.isSuccess(), score, reason);
    }

    private String extractScoreFromReason(String reason) {
        if (reason == null || !reason.contains("score=")) {
            return "-1";
        }
        try {
            String afterScore = reason.substring(reason.indexOf("score=") + 6);
            String numberOnly = afterScore.split("[^0-9]")[0];
            return numberOnly.isEmpty() ? "-1" : numberOnly;
        } catch (Exception e) {
            return "-1";
        }
    }

    public record RelevanceCheckResult(boolean passed, String score, String reason) {}
}