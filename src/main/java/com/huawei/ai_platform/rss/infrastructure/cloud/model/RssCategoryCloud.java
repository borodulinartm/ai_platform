package com.huawei.ai_platform.rss.infrastructure.cloud.model;

import lombok.*;

/**
 * Simple structure for the RSS category
 * Looks redundant, but I respect DDD very well :)
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssCategoryCloud {
    private int categoryId;
    private String categoryName;
}
