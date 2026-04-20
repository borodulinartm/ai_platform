package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AIStageResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
     * @param request - AI parameter request
     * @return AI parameter response
     *
     */
    public @Nonnull AIPipelineResponse executePipeline(@Nonnull AiPipelineRequest request) {
        if (!CollectionUtils.isEmpty(request.getStages())) {
            String userPayload = null;
            for (AiStage aiStage : request.getStages()) {
                if (userPayload != null) {
                    aiStage.getParameters().setUserPayload(userPayload);
                }

                IAiStageExecutor stageExecutor = aiStage.getExecutor();

                AIStageResponse responseForStage = stageExecutor.runStage(aiStage.getParameters());
                if (!responseForStage.isSuccess()) {
                    return AIPipelineResponse.failure(request.getName(), responseForStage.getFailureReason());
                }

                userPayload = responseForStage.getPayload();
            }

            return AIPipelineResponse.success(request.getName(), userPayload);
        } else {
            return AIPipelineResponse.success(request.getName(), StringUtils.EMPTY);
        }
    }
}
