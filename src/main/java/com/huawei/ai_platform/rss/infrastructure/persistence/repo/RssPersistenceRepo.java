package com.huawei.ai_platform.rss.infrastructure.persistence.repo;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.infrastructure.persistence.assembler.RssAssembler;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFetchData;
import com.huawei.ai_platform.rss.model.RssData;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import static com.huawei.ai_platform.common.Constant.ZONE;

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

    /**
     * Marks as read collection of the data
     *
     * @param rssDataCollection Collection of the RSS data
     * @return Operation Result: success if operation has completed with OK, failed otherwise
     */
    public OperationResult markAsRead(@NonNull Collection<Long> rssDataCollection) {
        rssDao.markAsReadNews(rssDataCollection);
        return OperationResult.builder().state(OperationResultEnum.SUCCESS).reason("Successfully read news").build();
    }
}
