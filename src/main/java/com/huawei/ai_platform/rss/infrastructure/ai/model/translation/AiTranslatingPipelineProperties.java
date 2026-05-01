package com.huawei.ai_platform.rss.infrastructure.ai.model.translation;

import com.huawei.ai_platform.rss.enums.AiTranslatingStagesEnum;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

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
    private List<StageParams> stages;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StageParams {
        private String model;
        private double temperature;
        private AiTranslatingStagesEnum stageName;
        private int countAttempts;
        private String systemPromptPathEn;
        private String systemPromptPathZh;

        private String userPromptPath;

        private StageParams validation;

        public String getSystemPrompt(Locale locale) {
            if (locale == Locale.ENGLISH) {
                return systemPromptPathEn;
            }

            return systemPromptPathZh;
        }
    }
}
