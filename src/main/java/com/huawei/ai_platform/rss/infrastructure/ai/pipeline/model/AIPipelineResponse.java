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
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AIPipelineResponse<T> {
    private boolean success;
    private String pipelineName;
    private T payload;
    private String failureReason;
}
