package com.huawei.ai_platform.rss.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArticleSummary(
    @JsonProperty("background") String background,
    @JsonProperty("eventSummary") String eventSummary,
    @JsonProperty("technologyAndInnovation") String technologyAndInnovation,
    @JsonProperty("valueAndImpact") String valueAndImpact,
    @JsonProperty("effects") String effects
) {}
