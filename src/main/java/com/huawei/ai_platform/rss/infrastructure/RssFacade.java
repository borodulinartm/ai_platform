package com.huawei.ai_platform.rss.infrastructure;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.repo.RssArticleTranslatorRepository;
import com.huawei.ai_platform.rss.application.repo.RssRepository;
import com.huawei.ai_platform.rss.infrastructure.ai.assembler.AiTranslationMapper;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.repo.AiTranslatorRepo;
import com.huawei.ai_platform.rss.infrastructure.cloud.assembler.RssArticleCloudAssembler;
import com.huawei.ai_platform.rss.infrastructure.cloud.assembler.RssCategoryCloudAssembler;
import com.huawei.ai_platform.rss.infrastructure.cloud.assembler.RssFeedCloudAssembler;
import com.huawei.ai_platform.rss.infrastructure.cloud.assembler.RssSummaryNewsAssembler;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssArticleCloud;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssCategoryCloud;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssFeedCloud;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssNewsSummaryCloud;
import com.huawei.ai_platform.rss.infrastructure.cloud.repo.RssArticlesUploader;
import com.huawei.ai_platform.rss.infrastructure.cloud.repo.RssCategoryUploader;
import com.huawei.ai_platform.rss.infrastructure.cloud.repo.RssFeedUploader;
import com.huawei.ai_platform.rss.infrastructure.cloud.repo.RssReportUploader;
import com.huawei.ai_platform.rss.infrastructure.persistence.assembler.RssAssembler;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFeedEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFetchData;
import com.huawei.ai_platform.rss.infrastructure.persistence.repo.RssPersistenceRepo;
import com.huawei.ai_platform.rss.model.RssCategory;
import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.rss.model.RssFeed;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
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
@Slf4j
public class RssFacade implements RssRepository, RssArticleTranslatorRepository {
    public static final int COUNT_THREADS = 4;

    private final RssPersistenceRepo persistenceRepo;
    private final RssAssembler rssAssembler;

    private final RssArticlesUploader cloudSender;
    private final RssArticleCloudAssembler rssArticleCloudAssembler;

    private final RssCategoryUploader rssCategoryUploader;
    private final RssCategoryCloudAssembler rssCategoryCloudAssembler;

    private final RssFeedUploader rssFeedUploader;
    private final RssFeedCloudAssembler rssFeedCloudAssembler;

    private final RssSummaryNewsAssembler rssSummaryNewsAssembler;
    private final RssReportUploader rssReportUploader;
    private final RssDao rssDao;
    private final AiTranslatorRepo aiTranslatorRepo;
    private final AiTranslationMapper aiTranslationMapper;

    @Override
    public List<RssData> getArticlesBy(@Nonnull LocalDateTime dateToFind) {
        return persistenceRepo.getArticles(dateToFind);
    }

    @Override
    public List<RssCategory> getListCategories() {
        List<RssCategoryEntity> categoryEntities =  persistenceRepo.getCategories();
        if (categoryEntities != null) {
            return rssAssembler.convertFromPersistenceToRssCategory(categoryEntities);
        }

        return Collections.emptyList();
    }

    @Override
    public List<RssFeed> getListFeeds() {
        List<RssFeedEntity> feedEntities = persistenceRepo.getFeedEntity();
        return rssAssembler.convertFromPersistenceToRssFeed(feedEntities);
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
    public OperationResult uploadReport(@Nonnull List<RssNewsSummary> newsSummaries, @Nonnull LocalDate reportDate) {
        List<RssNewsSummaryCloud> cloudNews = rssSummaryNewsAssembler.toSummaryCloud(newsSummaries);
        return rssReportUploader.uploadReport(cloudNews, reportDate);
    }

    @Override
    public OperationResult uploadCategories(@Nonnull Collection<RssCategory> categoryEntities) {
        List<RssCategoryCloud> cloudCategories = rssCategoryCloudAssembler.convert(categoryEntities);
        return rssCategoryUploader.uploadRssCategory(cloudCategories);
    }

    @Override
    public OperationResult uploadFeeds(@Nonnull Collection<RssFeed> feedEntities) {
        Collection<RssFeedCloud> rssFeedClouds = rssFeedCloudAssembler.convert(feedEntities);
        return rssFeedUploader.uploadRssFeed(rssFeedClouds);
    }

    @Override
    public List<RssData> translate(List<RssData> compacts) {
        if (CollectionUtils.isEmpty(compacts)) {
            throw new IllegalArgumentException("Array should not be empty");
        }

        int index = 0;
        int offset = 50;

        List<List<RssData>> rssItems = new ArrayList<>();

        while (index < compacts.size()) {
            List<RssData> subList = compacts.subList(index, Math.min(index + offset, compacts.size()));
            index += offset;

            rssItems.add(subList);
        }

        if (!rssItems.isEmpty()) {
            try (ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(COUNT_THREADS)) {
                List<List<RssData>> splittedList = rssItems.stream().limit(COUNT_THREADS).toList();

                List<ScheduledFuture<List<AiTranslationResponse>>> listFutures = new ArrayList<>();

                for (int i = 0; i < splittedList.size(); ++i) {
                    List<AiTranslationRequest> listRequests = splittedList.get(i).stream().map(aiTranslationMapper::convert)
                            .toList();
                    listFutures.add(scheduledExecutorService.schedule(
                            () -> aiTranslatorRepo.translate(listRequests),
                            i * 5L, TimeUnit.SECONDS // Big delay between tasks because rate limiting in the AI side
                    ));
                }

                for (ScheduledFuture<List<AiTranslationResponse>> item : listFutures) {
                    try {
                        List<AiTranslationResponse> response = item.get();
                        response.forEach(v -> log.info(v.toString()));
                    } catch (Exception exception) {
                        log.error("During extracting data there's an error: {}", exception.getMessage());
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public List<RssData> getNotTranslatedNews() {
        Long latestRegisteredArticle = rssDao.getMaxTranslatedTimestamp();
        List<RssFetchData> fetchDataList = rssDao.getAfter(latestRegisteredArticle);

        return rssAssembler.convertFromFetchToRssData(fetchDataList);
    }
}
