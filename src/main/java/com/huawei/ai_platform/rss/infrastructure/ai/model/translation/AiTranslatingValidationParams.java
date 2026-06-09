package com.huawei.ai_platform.rss.infrastructure.ai.model.translation;

import com.huawei.ai_platform.rss.enums.AiTranslatingStagesEnum;
import lombok.*;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Locale;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiTranslatingValidationParams {
    private String model;
    private double temperature;
    private AiTranslatingStagesEnum stageName;
    private int countAttempts;
    private String systemPromptPathEn;
    private String systemPromptPathZh;

    private String userPromptPath;

    public String getSystemPrompt(Locale locale) {
        if (locale == Locale.ENGLISH) {
            return systemPromptPathEn;
        }

        return systemPromptPathZh;
    }
}
