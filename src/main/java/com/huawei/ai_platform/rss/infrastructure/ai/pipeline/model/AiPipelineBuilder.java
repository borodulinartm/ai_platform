package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.IAiStageExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder pattern for the AI pipeline side
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiPipelineBuilder {
    private String pipelineName;
    private List<AiStage> stages;

    public static AiPipelineBuilder createBuilder(String pipelineName) {
        List<AiStage> stages = new ArrayList<>();
        return new AiPipelineBuilder(pipelineName, stages);
    }

    public AiPipelineBuilder addStage(long id, String stageName, IAiStageExecutor executor, String systemPrompt,
                                      String userPrompt, String model, double temperature, int maxAttempts) {
        AiStageParameters parameters = new AiStageParameters(id, stageName, systemPrompt, userPrompt, "", model, temperature,
                maxAttempts);
        stages.add(new AiStage(stageName, parameters, executor));

        return this;
    }

    public AiPipelineBuilder addStage(long id, String stageName, IAiStageExecutor executor, String systemPrompt,
                                      String userPrompt, String payload, String model, double temperature, int maxAttempts) {
        AiStageParameters parameters = new AiStageParameters(id, stageName, systemPrompt, userPrompt, payload, model, temperature,
                maxAttempts);
        stages.add(new AiStage(stageName, parameters, executor));

        return this;
    }

    public AiPipelineRequest build() {
        return new AiPipelineRequest(pipelineName, stages);
    }
}
