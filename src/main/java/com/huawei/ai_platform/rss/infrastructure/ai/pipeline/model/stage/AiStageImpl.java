package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;

import java.util.Set;
import java.util.function.Function;

public class AiStageImpl implements AiStage {
    private final String stageName;
    private final Set<AiTypedKey<?>> inputs;
    private final Set<AiTypedKey<?>> outputs;
    private final Function<AiPipelineContext, AIStageResponse> functionExtractor;

    public AiStageImpl(String stageName, Set<AiTypedKey<?>> inputs, Set<AiTypedKey<?>> outputs,
                       Function<AiPipelineContext, AIStageResponse> functionExtractor
    ) {
        this.stageName = stageName;
        this.inputs = inputs;
        this.outputs = outputs;
        this.functionExtractor = functionExtractor;
    }

    @Override
    public String stageName() {
        return stageName;
    }

    @Override
    public Set<AiTypedKey<?>> getInput() {
        return inputs;
    }

    @Override
    public Set<AiTypedKey<?>> getOutputs() {
        return outputs;
    }

    @Override
    public Function<AiPipelineContext, AIStageResponse> executorFunction() {
        return functionExtractor;
    }
}
