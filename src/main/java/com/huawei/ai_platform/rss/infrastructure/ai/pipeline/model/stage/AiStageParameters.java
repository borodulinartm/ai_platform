package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage;

import lombok.*;

import java.util.function.BiPredicate;

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
    private String model;
    // If we consider validation, we can use it directly into the stage executor. If the output result is not good, then throw an exception
    private BiPredicate<String, String> validationPredicate;
    private double temperature;
    private int maxAttempts;
}
