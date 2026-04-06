package com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning;

import lombok.*;

/**
 * Cleaning response for the AI section
 *
 * @author Borodulin Artem
 * @since 2026.04.04
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiCleaningResponse {
    private Long id;
    private String articleTitleCleaned;
    private String articleContentCleaned;
    private String articleLink;
    private boolean success;

    public static AiCleaningResponse success(Long id, String title, String content, String articleLink) {
        return new AiCleaningResponse(id, title, content, articleLink, true);
    }

    public static AiCleaningResponse failure(Long id) {
        return new AiCleaningResponse(id, "", "", "", false);
    }
}
