package com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping;

import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssAttributeValue;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Scrapped response for the AI section
 *
 * @author Borodulin Artem
 * @since 2026.04.04
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiScrappingResponse {
    private Long id;
    private String articleTitle;
    private String articleContentScrapped;
    private RssAttributeValue rssAttributeValue;
    private String articleLink;
    private boolean success;
    private String reason;

    /**
     * Static factory for the success state
     *
     * @param id             id
     * @param title          title
     * @param content        content
     * @param articleLink    link to the data
     * @param attributeValue attribute value
     * @return Ai cleaning response
     */
    public static AiScrappingResponse success(Long id, String title, String content, String articleLink,
                                              RssAttributeValue attributeValue) {
        return new AiScrappingResponse(id, title, content, attributeValue, articleLink, true, StringUtils.EMPTY);
    }

    /**
     * Static factory for the failed situation
     *
     * @param id     id
     * @param reason why error has occurred
     * @return Ai cleaned response
     */
    public static AiScrappingResponse failure(Long id, String reason) {
        return new AiScrappingResponse(id, "", "", null, "", false, reason);
    }
}
