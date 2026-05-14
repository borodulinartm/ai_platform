package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.enums.AiResultEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * AI result text (low-level response structure). Contain raw data
 *
 * @author Borodulin Artem
 * @since 2026.05.13
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class AiDriverResponse {
    private AiResultEnum resultEnum;
    private String text;
}
