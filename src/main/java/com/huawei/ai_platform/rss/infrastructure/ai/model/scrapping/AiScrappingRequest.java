package com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping;

import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssAttributeValue;
import lombok.*;

/**
 * Scrapping request model for the AI (copied from the AiCleaningRequest). Maybe in the future it'll extend
 *
 * @author Borodulin Artem
 * @since 2026.04.16
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiScrappingRequest {
    private Long id;
    private String articleTitle;
    private String articleContent;
    private String articleLink;
    private RssAttributeValue attributes;
}
