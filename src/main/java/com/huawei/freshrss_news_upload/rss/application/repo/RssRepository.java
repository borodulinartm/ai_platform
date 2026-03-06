package com.huawei.freshrss_news_upload.rss.application.repo;

import com.huawei.freshrss_news_upload.rss.model.RssData;
import com.huawei.freshrss_news_upload.common.OperationResult;

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
     * @param dateToSend
     * @return OperationResult: SUCCESS/FAILURE
     */
    OperationResult sendToCloud(Collection<RssData> rssData, LocalDateTime dateToSend);
}
