package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import jakarta.annotation.Nonnull;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for syncing subsystem
 *
 * @author Borodulin Artem
 * @since 2026.03.12
 */
public interface RssSyncService {
    /**
     * Uploads articles
     *
     * @return OperationResult: success/failure
     */
    OperationResult uploadNewArticles();

    /**
     * Uploads report
     *
     * @param reports    List of reports
     * @param reportDate For which date do you want uploading
     * @return OperationResult: success/failure
     */
    OperationResult uploadReport(@Nonnull List<RssNewsSummary> reports, @Nonnull LocalDate reportDate);
}
