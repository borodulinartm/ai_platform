package com.huawei.ai_platform.rss.infrastructure.ai.model.translation;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI data pipeline information
 *
 * @author Borodulin Artem
 * @since 2026.05.01
 */
@Getter
@Setter
@Component
@ConfigurationProperties(value = "ai.translating")
public class AiTranslatingPipelineProperties {
    private List<AiTranslatingStageParams> stages;
}
