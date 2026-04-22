package com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning;

import lombok.*;

/**
 * Request for relevance check containing only necessary fields
 *
 * @author Borodulin Artem
 * @since 2026.04.22
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RelevanceCheckRequest {
    private long id;
    private String title;
    private String content;
    private String categoryName;
}