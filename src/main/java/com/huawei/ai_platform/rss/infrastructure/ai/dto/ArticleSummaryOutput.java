package com.huawei.ai_platform.rss.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ArticleSummaryOutput(
    String title,
    @JsonProperty("abstract") String abstractField,
    String articleLink,
    List<String> authors,
    String titleCn,
    String abstractCn,
    String background,
    String effects,
    String eventSummary,
    String technologyAndInnovation,
    String valueAndImpact,
    String backgroundCn,
    String effectsCn,
    String eventSummaryCn,
    String technologyAndInnovationCn,
    String valueAndImpactCn
) {}
