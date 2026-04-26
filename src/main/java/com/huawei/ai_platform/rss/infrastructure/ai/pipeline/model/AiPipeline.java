package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

/**
 * Structure for storing AI pipeline input param info
 *
 * @author Borodulin Artem
 * @since 2026.04.24
 */
@Getter
@Setter
@AllArgsConstructor
public class AiPipeline<I, O> {
    private String pipelineName;
    private AiTypedKey<I> input;
    private AiTypedKey<O> output;

    private List<AiStage> stages;
}
