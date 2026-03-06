package com.huawei.freshrss_news_upload.rss.infrastructure;

import com.huawei.freshrss_news_upload.rss.infrastructure.persistence.repo.RssPersistenceRepo;
import com.huawei.freshrss_news_upload.rss.model.RssData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
public class RssFacade {
    private final RssPersistenceRepo persistenceRepo;

    public List<RssData> getUnreadRssDataBy(LocalDateTime dateToFind) {
        return persistenceRepo.getUnreadRssDataBy(dateToFind);
    }
}
