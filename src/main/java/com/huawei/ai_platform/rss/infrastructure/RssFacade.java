package com.huawei.ai_platform.rss.infrastructure;

import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.repo.RssRepository;
import com.huawei.ai_platform.rss.infrastructure.cloud.RssCloudSender;
import com.huawei.ai_platform.rss.infrastructure.persistence.repo.RssPersistenceRepo;
import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.common.OperationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public OperationResult markAsRead(Collection<RssData> rssData) {
        try {
            Set<Long> arrayIds = rssData.stream().map(RssData::getItemId).collect(Collectors.toSet());
            return persistenceRepo.markAsRead(arrayIds);
        } catch (Exception e) {
            return OperationResult.builder().state(OperationResultEnum.FAILURE)
                    .reason("An exception has occurred. Reason = " + e.getMessage()).build();
        }
    }
}
