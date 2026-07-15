package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.common.OperationResult;
import jakarta.annotation.Nonnull;

import java.time.LocalDate;

public interface RssTopArticlesService {
    /**
     * Processes TOP-N articles per category and generates reports
     *
     * @param forWhichDate date for processing
     * @return OperationResult with status
     */
    OperationResult processTopArticles(@Nonnull LocalDate forWhichDate);
}
