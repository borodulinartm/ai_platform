package com.huawei.freshrss_news_upload.rss.infrastructure;

import com.huawei.freshrss_news_upload.rss.application.repo.RssRepository;
import com.huawei.freshrss_news_upload.rss.infrastructure.cloud.RssCloudSender;
import com.huawei.freshrss_news_upload.rss.infrastructure.persistence.repo.RssPersistenceRepo;
import com.huawei.freshrss_news_upload.rss.model.RssData;
import com.huawei.freshrss_news_upload.common.OperationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Facade class for working with RSS. Encapsulates 'Repo' abstract layer
 * Here, we can interact with persistence and another parts, like FS, Kafka, etc.
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Component
@RequiredArgsConstructor
public class RssFacade implements RssRepository {
    private final RssPersistenceRepo persistenceRepo;
    private final RssCloudSender cloudSender;

    @Override
    public List<RssData> getUnreadItemsBy(LocalDateTime dateToFind) {
        return persistenceRepo.getUnreadRssDataBy(dateToFind);
    }

    @Override
    public OperationResult sendToCloud(Collection<RssData> rssData, LocalDateTime dateToSend) {
        return cloudSender.upload(rssData, dateToSend);
    }
}
