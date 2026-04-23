package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import lombok.*;

import java.util.List;

/**
 * AI pipeline parameter section
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiPipelineRequest {
    private String name;
    private String payload;
    private List<AiStage> stages;
}
