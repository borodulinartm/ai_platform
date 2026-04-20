package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.IAiStageExecutor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * AI stage abstraction
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@Getter
@Setter
@AllArgsConstructor
public class AiStage {
    private String stageName;
    private AiStageParameters parameters;
    private IAiStageExecutor executor;
}
