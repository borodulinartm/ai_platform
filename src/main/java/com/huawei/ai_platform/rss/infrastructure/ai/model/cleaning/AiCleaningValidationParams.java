package com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning;

import com.huawei.ai_platform.rss.enums.AiCleaningStagesEnum;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiCleaningValidationParams {
    private String model;
    private double temperature;
    private AiCleaningStagesEnum stageName;
    private int countAttempts;
    private String systemPromptPath;
    private String userPromptPath;
}
