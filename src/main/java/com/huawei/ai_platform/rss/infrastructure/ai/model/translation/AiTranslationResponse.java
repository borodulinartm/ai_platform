package com.huawei.ai_platform.rss.infrastructure.ai.model.translation;

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
    private String reason;
    private String articleTitleEn;
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
     * @param articleTitleEn   title (translated to EN)
     * @param articleTitleZh   title (translated to ZH)
     * @param articleContentEn (content in EN, not translated)
     * @param articleContentZh (content in ZH, translated)
     * @return wrapper
     */
    public static AiTranslationResponse successResponse(long articleId, String articleTitleEn, String articleTitleZh,
                                                        String articleContentEn, String articleContentZh) {
        return new AiTranslationResponse(articleId, true, StringUtils.EMPTY, articleTitleEn, articleTitleZh,
                articleContentZh, articleContentEn
        );
    }

    /**
     * Static factory method for failure situations
     *
     * @param articleId article id
     * @param reason    why this happened
     * @return Ai translation response
     */
    public static AiTranslationResponse failureResponse(long articleId, String reason) {
        return new AiTranslationResponse(articleId, false, reason, StringUtils.EMPTY, StringUtils.EMPTY,
                StringUtils.EMPTY, StringUtils.EMPTY
        );
    }
}
