package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder pattern for the AI pipeline side
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiPipelineBuilder<I, O> {
    private String pipelineName;

    private AiTypedKey<I> input;
    private AiTypedKey<O> output;

    private List<AiStage<?>> stages;

    public AiPipelineBuilder<I, O> addStage(AiStage<?> stage) {
        stages.add(stage);

        return this;
    }

    public AiPipeline<I, O> build() {
        return new AiPipeline<>(pipelineName, input, output, stages);
    }

    /**
     * Static factory method of creation
     *
     * @param pipelineName pipeline name
     * @return builder instance
     */
    public static <I, O> AiPipelineBuilder<I, O> withName(String pipelineName, AiTypedKey<I> input, AiTypedKey<O> output) {
        return new AiPipelineBuilder<>(pipelineName, input, output, new ArrayList<>());
    }
}
