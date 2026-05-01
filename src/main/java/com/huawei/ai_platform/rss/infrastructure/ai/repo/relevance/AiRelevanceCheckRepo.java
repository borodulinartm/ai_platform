package com.huawei.ai_platform.rss.infrastructure.ai.repo.relevance;

import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.RelevanceCheckRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiPipelineExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipeline;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.factory.AiUnaryStageFactory;
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
    private static final String PIPELINE_NAME = "RELEVANCE_CHECK";
    private static final String RELEVANCE_PROMPT = "prompt/relevance/relevance-check-prompt.txt";
    private static final String USER_PROMPT = "prompt/user-prompt.txt";

    private static final AiTypedKey<String> RELEVANCE_INPUT = AiTypedKey.of(String.class, "RELEVANCE_INPUT");
    private static final AiTypedKey<String> RELEVANCE_OUTPUT = AiTypedKey.of(String.class, "RELEVANCE_OUTPUT");

    private final AiFunction1Executor<String, String> relevanceStageExecutor;
    private final AiPipelineExecutor aiPipelineExecutor;

    // AI relevance parameters
    @Value("${ai.relevance.countAttempts}")
    private int maxCountAttemptsRelevance;
    @Value("${ai.relevance.temperature}")
    private Double temperatureRelevance;
    @Value("${ai.relevance.model}")
    private String modelRelevance;

    public RelevanceCheckResult checkRelevance(RelevanceCheckRequest request) {
        String payload = String.format("Category: %s\nTitle: %s\nContent: %s",
                request.getCategoryName() != null ? request.getCategoryName() : "",
                request.getTitle() != null ? request.getTitle() : "",
                request.getContent() != null ? request.getContent() : "");

        AiPipeline<String, String> pipelineRequest = AiPipelineBuilder.withName(PIPELINE_NAME, RELEVANCE_INPUT, RELEVANCE_OUTPUT)
                .addStage(addRelevanceCheckingPrompt(request)).build();

        AIPipelineResponse<String> pipelineResponse = aiPipelineExecutor.executePipeline(pipelineRequest, payload);

        if (pipelineResponse.isSuccess()) {
            int score = Integer.parseInt(pipelineResponse.getPayload().trim());
            return new RelevanceCheckResult(true, score, pipelineResponse.getPayload());
        } else {
            return new RelevanceCheckResult(false, -1, pipelineResponse.getFailureReason());
        }
    }

    private AiStage<?> addRelevanceCheckingPrompt(RelevanceCheckRequest relevanceCheckRequest) {
        String stageName = "RELEVANCE_STAGE";

        AiStageParameters stageParameters = new AiStageParameters(stageName,
                relevanceCheckRequest.getId(), RELEVANCE_PROMPT, USER_PROMPT, modelRelevance, temperatureRelevance, maxCountAttemptsRelevance
        );

        return new AiUnaryStageFactory().createStage(stageName, RELEVANCE_INPUT, RELEVANCE_OUTPUT, stageParameters,
                relevanceStageExecutor, null, null
        );
    }

    public record RelevanceCheckResult(boolean passed, int score, String reason) {}
}