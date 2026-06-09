package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Pipeline builder of AI response
 *
 * @author Borodulin Artem
 * @since 2026.04.26
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AiPipelineResponseBuilder<T> {
    private boolean success;
    private String pipelineName;
    private T payload;
    private String failureReason;

    public AiPipelineResponseBuilder(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public AiPipelineResponseBuilder<T> failure(String failureReason) {
        this.success = false;
        this.failureReason = failureReason;

        return this;
    }

    public AiPipelineResponseBuilder<T> success(T payload) {
        this.success = true;
        this.failureReason = null;
        this.payload = payload;

        return this;
    }

    public AIPipelineResponse<T> build() {
        return new AIPipelineResponse<>(success, pipelineName, payload, failureReason);
    }
}
