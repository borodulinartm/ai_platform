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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern SCORE_PATTERN = Pattern.compile("score=(\\d+)");

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
        int score = extractScore(pipelineResponse.getFailureReason());
        return new RelevanceCheckResult(pipelineResponse.isSuccess(), score, pipelineResponse.getFailureReason());
    }

    private int extractScore(String failureReason) {
        if (failureReason == null) {
            return -1;
        }
        Matcher matcher = SCORE_PATTERN.matcher(failureReason);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    public record RelevanceCheckResult(boolean passed, int score, String reason) {}
}