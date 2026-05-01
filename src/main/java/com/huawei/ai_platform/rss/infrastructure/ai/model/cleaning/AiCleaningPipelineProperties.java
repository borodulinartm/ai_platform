package com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning;

import com.huawei.ai_platform.rss.enums.AiCleaningStagesEnum;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI data stage information
 *
 * @author Borodulin Artem
 * @since 2026.05.01
 */
@Getter
@Setter
@Component
@ConfigurationProperties(value = "ai.cleaning")
public class AiCleaningPipelineProperties {
    private List<StageParams> stages;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StageParams {
        private String model;
        private double temperature;
        private AiCleaningStagesEnum stageName;
        private int countAttempts;
        private String systemPromptPath;
        private String userPromptPath;

        private StageParams validation;
    }
}
