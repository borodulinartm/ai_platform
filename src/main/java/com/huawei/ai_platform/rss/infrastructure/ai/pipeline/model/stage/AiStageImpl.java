package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.IAiStageValidation;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineContext;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;

import java.util.Set;
import java.util.function.Function;

public class AiStageImpl<O> implements AiStage<O> {
    private final String stageName;
    private final Set<AiTypedKey<?>> inputs;
    private final Set<AiTypedKey<?>> outputs;
    private final Function<AiPipelineContext, AIStageResponse<O>> functionExtractor;
    private final AiStageParameters aiStageParameters;
    private final IAiStageValidation<?, O> validation;

    public AiStageImpl(String stageName, Set<AiTypedKey<?>> inputs, Set<AiTypedKey<?>> outputs,
                       Function<AiPipelineContext, AIStageResponse<O>> functionExtractor,
                       AiStageParameters aiStageParameters, IAiStageValidation<?, O> validation
    ) {
        this.stageName = stageName;
        this.inputs = inputs;
        this.outputs = outputs;
        this.functionExtractor = functionExtractor;
        this.aiStageParameters = aiStageParameters;
        this.validation = validation;
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
    public AiStageParameters getStageParameters() {
        return aiStageParameters;
    }

    @Override
    public Function<AiPipelineContext, AIStageResponse<O>> executorFunction() {
        return functionExtractor;
    }

    @Override
    public IAiStageValidation<?, O> validation() {
        return validation;
    }
}
