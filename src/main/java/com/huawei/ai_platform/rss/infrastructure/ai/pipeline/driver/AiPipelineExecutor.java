package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipeline;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineResponseBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AIStageResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Low-level driver class for executing AI pipeline
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@Component
@RequiredArgsConstructor
public class AiPipelineExecutor {

    /**
     * Central method for executing pipeline
     *
     * @param pipeline pipeline config
     */
    public <I, O> AIPipelineResponse<O> executePipeline(@Nonnull AiPipeline<I, O> pipeline, I input) {
        AiPipelineContext context = new AiPipelineContext();
        context.addStageResult(pipeline.getInput(), input);

        // Add validation

        if (!CollectionUtils.isEmpty(pipeline.getStages())) {
            for (AiStage<?> aiStage : pipeline.getStages()) {
                AIStageResponse<?> response = aiStage.executorFunction().apply(context);
                if (!response.isSuccess()) {
                    AiPipelineResponseBuilder<O> responseBuilder = new AiPipelineResponseBuilder<>(pipeline.getPipelineName());
                    responseBuilder.failure(String.format(
                            "AI stage %s has finished wih failure. Reason = %s",
                            aiStage.stageName(), response.getFailureReason()
                    ));

                    return responseBuilder.build();
                }
            }
        } else {
            return new AiPipelineResponseBuilder<O>(pipeline.getPipelineName())
                    .failure("Pipeline is empty. Returning").build();
        }

        try {
            O outputResult = context.getStageResult(pipeline.getOutput());
            return new AiPipelineResponseBuilder<O>(pipeline.getPipelineName()).success(outputResult).build();
        } catch (Exception exception) {
            return new AiPipelineResponseBuilder<O>(pipeline.getPipelineName())
                    .failure(String.format("An exception has occurred during returning output value: %s", exception.getMessage()))
                    .build();
        }
    }
}
