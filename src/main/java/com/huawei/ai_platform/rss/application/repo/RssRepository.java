package com.huawei.ai_platform.rss.application.repo;

import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.common.OperationResult;

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
    List<RssData> getUnreadItemsBy(LocalDateTime dateToFind);

    /**
     * Performs sending to Rss Data
     *
     * @param rssData    rss data item
     * @param dateToSend date when it needs to send
     * @return OperationResult: SUCCESS/FAILURE
     */
    OperationResult sendToCloud(Collection<RssData> rssData, LocalDateTime dateToSend);

    /**
     * Performs marking as read
     *
     * @param rssData rss data
     * @return OperationResult: success if operation has completed with OK, failed otherwise
     */
    OperationResult markAsRead(Collection<RssData> rssData);
}
