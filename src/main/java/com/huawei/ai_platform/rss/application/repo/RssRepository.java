package com.huawei.ai_platform.rss.application.repo;

import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssNewsSummaryCloud;
import com.huawei.ai_platform.rss.model.RssCategory;
import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.model.RssFeed;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import jakarta.annotation.Nonnull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Base RSS repository
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
public interface RssRepository {
    /**
     * Extracts unread items by specified date
     *
     * @param dateToFind provided date
     * @return list of the rss data
     */
    List<RssData> getArticlesBy(@Nonnull LocalDateTime dateToFind);

    /**
     * Extracts list of categories
     *
     * @return List of categories
     */
    List<RssCategory> getListCategories();

    /**
     * Extracts list of feeds
     *
     * @return List of feeds
     */
    List<RssFeed> getListFeeds();

    /**
     * Performs sending to Rss Data
     *
     * @param rssData    rss data item
     * @param dateToSend date to send
     * @return OperationResult: SUCCESS/FAILURE
     */
    OperationResult uploadArticles(@Nonnull Collection<RssData> rssData, LocalDateTime dateToSend);

    /**
     * Performs uploading categories to cloud
     *
     * @param categoryEntities collection of the categories
     * @return OperationResult: success/failure
     */
    OperationResult uploadCategories(@Nonnull Collection<RssCategory> categoryEntities);

    /**
     * Performs uploading feeds to cloud
     *
     * @param feedEntities collection of the categories
     * @return OperationResult: success/failure
     */
    OperationResult uploadFeeds(@Nonnull Collection<RssFeed> feedEntities);

    /**
     * Performs marking as read
     *
     * @param rssData rss data
     * @return OperationResult: success if operation has completed with OK, failed otherwise
     */
    OperationResult markAsRead(@Nonnull Collection<RssData> rssData);

    /**
     * Performs uploading to server for the news summaries
     *
     * @param newsSummaries news summaries
     * @param reportDate    for which date do you want to make report
     * @return OperationResult: success if good, fail otherwise
     */
    OperationResult uploadReport(@Nonnull List<RssNewsSummary> newsSummaries, @Nonnull LocalDate reportDate);

    OperationResult uploadCloudReport(@Nonnull List<RssNewsSummaryCloud> newsSummaries, @Nonnull LocalDate reportDate);
}
