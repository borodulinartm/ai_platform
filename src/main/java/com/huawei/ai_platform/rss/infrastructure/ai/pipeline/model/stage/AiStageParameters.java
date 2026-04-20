package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import lombok.*;

/**
 * Inner section - AI stage parameter. For each stage describe prompt, user data and business logic for execution
 *
 * @author Borodulin Artem
 * @since 2026.04.20
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiStageParameters {
    private long id;
    private String name;
    private String systemPrompt;
    private String userPrompt;
    private String userPayload;
    private String model;
    private double temperature;
    private int maxAttempts;
}
