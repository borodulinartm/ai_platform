package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.RssAttributeTypeEnum;
import lombok.*;

import java.util.List;

/**
 * Value object for the rss attributing
 *
 * @author Borodulin Artem
 * @since 2026.04.13
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RssAttributeValue {
    private List<Enclosure> enclosures;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Enclosure {
        private String url;
        private RssAttributeTypeEnum type;
        private long length;
    }
}
