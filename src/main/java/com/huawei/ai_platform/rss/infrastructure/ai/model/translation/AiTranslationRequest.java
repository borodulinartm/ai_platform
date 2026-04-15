package com.huawei.ai_platform.rss.infrastructure.ai.model.translation;

import lombok.*;

/**
 * Model class for storing AI translation request
 *
 * @author Borodulin Artem
 * @since 2026.03.23
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiTranslationRequest {
    private long articleId;
    private String articleTitle;
    private String articleContent;
    private String articleLink;
}

