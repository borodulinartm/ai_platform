package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Pipeline context data
 *
 * @author Borodulin Artem
 * @since 2026.04.21
 */
public class AiPipelineContext {
    private Map<AiTypedKey<?>, Object> mapData;

    public AiPipelineContext() {
        mapData = new HashMap<>();
    }

    public <T> void addStageResult(AiTypedKey<T> key, T value) {
        mapData.put(key, value);
    }

    public <T> T getStageResult(AiTypedKey<T> className) {
        if (className == null) {
            throw new IllegalArgumentException("Class exists");
        }

        if (!mapData.containsKey(className)) {
            throw new IllegalStateException("There's no data for this class name");
        }

        Object value = mapData.get(className);
        return className.getType().cast(value);
    }
}
