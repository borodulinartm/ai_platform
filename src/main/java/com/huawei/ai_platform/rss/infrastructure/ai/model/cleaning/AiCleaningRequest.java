package com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning;

import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssAttributeValue;
import lombok.*;

import java.util.List;

/**
 * Cleaning request for the AI
 *
 * @author Borodulin Artem
 * @since 2026.04.04
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiCleaningRequest {
    private Long id;
    private String articleTitle;
    private String articleContent;
    private String articleLink;
    private RssAttributeValue attributes;
}
