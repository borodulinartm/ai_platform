package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Pipeline context data
 *
 * @author Borodulin Artem
 * @since 2026.04.21
 */
public class AiPipelineContext {
    private Map<String, String> mapStageOutput;

    @Getter
    @Setter
    private String initial;

    @Getter
    @Setter
    private String previousStage;

    public AiPipelineContext() {
        mapStageOutput = new HashMap<>();
        initial = "";
        previousStage = "";
    }

    public void addStageResult(String key, String value) {
        mapStageOutput.put(key, value);
    }

    public String getStageResult(String key) {
        return mapStageOutput.get(key);
    }

}
