package com.huawei.ai_platform.rss.application.repo;

import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFeedEntity;
import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.common.OperationResult;
import jakarta.annotation.Nonnull;

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
    List<RssData> getUnreadItemsBy(@Nonnull LocalDateTime dateToFind);

    /**
     * Extracts list of categories
     *
     * @return List of categories
     */
    List<RssCategoryEntity> getListCategories();

    /**
     * Extracts list of feeds
     *
     * @return List of feeds
     */
    List<RssFeedEntity> getListFeeds();

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
    OperationResult uploadCategories(@Nonnull Collection<RssCategoryEntity> categoryEntities);

    /**
     * Performs uploading feeds to cloud
     *
     * @param feedEntities collection of the categories
     * @return OperationResult: success/failure
     */
    OperationResult uploadFeeds(@Nonnull Collection<RssFeedEntity> feedEntities);

    /**
     * Performs marking as read
     *
     * @param rssData rss data
     * @return OperationResult: success if operation has completed with OK, failed otherwise
     */
    OperationResult markAsRead(@Nonnull Collection<RssData> rssData);
}
