package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Response section for entire AI pipeline. Describes statuses and reason
 * Currently, duplicates {@link com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AIStageResponse}
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AIPipelineResponse {
    private boolean success;
    private String pipelineName;
    private String payload;
    private String failureReason;

    public static AIPipelineResponse success(String pipelineName, String payload) {
        return new AIPipelineResponse(true, pipelineName,  payload,"");
    }

    public static AIPipelineResponse failure(String pipelineName, String reason) {
        return new AIPipelineResponse(false, pipelineName, "", reason);
    }
}
