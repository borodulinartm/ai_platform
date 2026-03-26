package com.huawei.ai_platform.rss.infrastructure.ai.model;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

/**
 * Model class for storing AI translation response
 *
 * @author Borodulin Artem
 * @since 2026.03.23
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AiTranslationResponse {
    private long articleId;
    private boolean success;
    private String articleTitleZh;
    private String articleContentZh;
    private String articleContentEn;

    @Override
    public String toString() {
        return "articleId = " + articleId + "; articleContentZh = " + articleContentZh;
    }

    // static factory methods

    /**
     * Static method for success situations
     *
     * @param articleId        article id
     * @param title            title (translated)
     * @param articleContentEn (content in EN, not translated)
     * @param articleContentZh (content in ZH, translated)
     * @return wrapper
     */
    public static AiTranslationResponse successResponse(long articleId, String title, String articleContentEn,
                                                        String articleContentZh) {
        return new AiTranslationResponse(articleId, true, title, articleContentZh, articleContentEn);
    }

    /**
     * Static factory method for failure situations
     *
     * @param articleId article id
     * @return Ai translation response
     */
    public static AiTranslationResponse failureResponse(long articleId) {
        return new AiTranslationResponse(articleId, false, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
    }
}
