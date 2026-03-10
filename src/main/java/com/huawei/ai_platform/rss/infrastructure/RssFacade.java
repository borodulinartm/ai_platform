package com.huawei.ai_platform.rss.infrastructure;

import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.repo.RssRepository;
import com.huawei.ai_platform.rss.infrastructure.cloud.assembler.RssArticleCloudAssembler;
import com.huawei.ai_platform.rss.infrastructure.cloud.assembler.RssCategoryCloudAssembler;
import com.huawei.ai_platform.rss.infrastructure.cloud.assembler.RssFeedCloudAssembler;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssArticleCloud;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssCategoryCloud;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssFeedCloud;
import com.huawei.ai_platform.rss.infrastructure.cloud.repo.RssArticlesUploader;
import com.huawei.ai_platform.rss.infrastructure.cloud.repo.RssCategoryUploader;
import com.huawei.ai_platform.rss.infrastructure.cloud.repo.RssFeedUploader;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFeedEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.repo.RssPersistenceRepo;
import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.common.OperationResult;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private final RssArticlesUploader cloudSender;
    private final RssArticleCloudAssembler rssArticleCloudAssembler;

    private final RssCategoryUploader rssCategoryUploader;
    private final RssCategoryCloudAssembler rssCategoryCloudAssembler;

    private final RssFeedUploader rssFeedUploader;
    private final RssFeedCloudAssembler rssFeedCloudAssembler;

    @Override
    public List<RssData> getUnreadItemsBy(@Nonnull LocalDateTime dateToFind) {
        return persistenceRepo.getUnreadRssDataBy(dateToFind);
    }

    @Override
    public List<RssCategoryEntity> getListCategories() {
        return persistenceRepo.getCategories();
    }

    @Override
    public List<RssFeedEntity> getListFeeds() {
        return persistenceRepo.getFeedEntity();
    }

    @Override
    public OperationResult uploadArticles(@Nonnull Collection<RssData> rssData, LocalDateTime dateToSend) {
        List<RssArticleCloud> converted = new ArrayList<>();
        rssData.forEach(v -> converted.add(rssArticleCloudAssembler.convertToCloudStructure(v)));

        return cloudSender.upload(converted, dateToSend);
    }

    @Override
    public OperationResult markAsRead(@Nonnull Collection<RssData> rssData) {
        try {
            Set<Long> arrayIds = rssData.stream().map(RssData::getArticleId).collect(Collectors.toSet());
            return persistenceRepo.markAsRead(arrayIds);
        } catch (Exception e) {
            return OperationResult.builder().state(OperationResultEnum.FAILURE)
                    .reason("An exception has occurred. Reason = " + e.getMessage()).build();
        }
    }

    @Override
    public OperationResult uploadCategories(@Nonnull Collection<RssCategoryEntity> categoryEntities) {
        List<RssCategoryCloud> cloudCategories = rssCategoryCloudAssembler.convert(categoryEntities);
        return rssCategoryUploader.uploadRssCategory(cloudCategories);
    }

    @Override
    public OperationResult uploadFeeds(@Nonnull Collection<RssFeedEntity> feedEntities) {
        Collection<RssFeedCloud> cloudCategories = rssFeedCloudAssembler.convert(feedEntities);
        return rssFeedUploader.uploadRssCategory(cloudCategories);
    }
}
