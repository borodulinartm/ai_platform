package com.huawei.ai_platform.rss.infrastructure.ai.dto;

public record ArticleRanking(
    long id,
    double score,
    String reason
) {}
