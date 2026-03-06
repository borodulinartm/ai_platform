package com.huawei.freshrss_news_upload.rss.infrastructure.persistence.repo;

import com.huawei.freshrss_news_upload.rss.infrastructure.persistence.assembler.RssAssembler;
import com.huawei.freshrss_news_upload.rss.infrastructure.persistence.dao.RssDao;
import com.huawei.freshrss_news_upload.rss.infrastructure.persistence.entity.RssFetchData;
import com.huawei.freshrss_news_upload.rss.model.RssData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.huawei.freshrss_news_upload.common.Constant.ZONE;

/**
 * Repository layer for RSS subside
 *
 * @author Borodulin Artem
 * @since 2026.03.05
 */
@Repository
@RequiredArgsConstructor
public class RssPersistenceRepo {
    private final RssDao rssDao;
    private final RssAssembler rssAssembler;

    /**
     * Extracts list of RSS data items
     *
     * @param dateToFind for which date do you want to find records?
     * @return list of RSS data
     */
    public List<RssData> getUnreadRssDataBy(LocalDateTime dateToFind) {
        long milliStart = dateToFind.with(LocalTime.MIN).atZone(ZONE).toEpochSecond();
        long milliEnd = dateToFind.with(LocalTime.MAX).atZone(ZONE).toEpochSecond();

        List<RssFetchData> fetchedData = rssDao.queryUnreadItemsByDate(milliStart, milliEnd);
        return rssAssembler.convertFromFetchToRssData(fetchedData);
    }
}
