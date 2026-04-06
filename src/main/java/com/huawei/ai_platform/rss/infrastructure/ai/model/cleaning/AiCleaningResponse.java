package com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

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
    private String reason;

    /**
     * Static factory for the success state
     *
     * @param id          id
     * @param title       title
     * @param content     content
     * @param articleLink link to the data
     * @return Ai cleaning response
     */
    public static AiCleaningResponse success(Long id, String title, String content, String articleLink) {
        return new AiCleaningResponse(id, title, content, articleLink, true, StringUtils.EMPTY);
    }

    /**
     * Static factory for the failed situation
     *
     * @param id     id
     * @param reason why error has occurred
     * @return Ai cleaned response
     */
    public static AiCleaningResponse failure(Long id, String reason) {
        return new AiCleaningResponse(id, "", "", "", false, reason);
    }
}
