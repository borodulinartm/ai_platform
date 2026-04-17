package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Value object class for storing attributes for the category
 *
 * @author Borodulin Artem
 * @since 2026.04.17
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RssCategoryAttribute {
    private int position;
}
