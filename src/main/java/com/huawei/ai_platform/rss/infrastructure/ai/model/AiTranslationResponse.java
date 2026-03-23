package com.huawei.ai_platform.rss.infrastructure.ai.model;

import lombok.*;

/**
 * Model class for storing AI translation response
 *
 * @author Borodulin Artem
 * @since 2026.03.23
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiTranslationResponse {
    private long articleId;
    private String articleTitleZh;
    private String articleContentZh;

    @Override
    public String toString() {
        return "articleId = " + articleId + "; articleContentZh = " + articleContentZh;
    }
}
