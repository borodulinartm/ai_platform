package com.huawei.ai_platform.rss.infrastructure.ai.dto;

import java.util.List;

public record CategorySummaryOutput(
    int categoryId,
    String articleTopSummaryEn,
    String articleTopSummaryZh,
    List<ArticleSummaryOutput> articles
) {}
