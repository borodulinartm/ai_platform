package com.huawei.ai_platform.rss.application.service.impl;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.repo.RssArticleTranslatorRepository;
import com.huawei.ai_platform.rss.application.repo.RssRepository;
import com.huawei.ai_platform.rss.application.service.RssConfigService;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import com.huawei.ai_platform.rss.application.service.RssTranslationOrchestration;
import com.huawei.ai_platform.rss.application.service.RssTranslationService;
import com.huawei.ai_platform.rss.infrastructure.ai.assembler.AiTranslationMapper;
import com.huawei.ai_platform.rss.infrastructure.ai.executor.ArticleDedupService;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import com.huawei.ai_platform.rss.model.RssCategory;
import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.rss.model.RssFeed;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.concurrent.*;

import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.FAILURE;
import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.INIT;
import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.SKIPPED;

/**
 * Business logic layer
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RssServiceImpl implements RssSyncService, RssConfigService, RssTranslationService {
    @Value("${ai.settings.countConcurrentConnections}")
    private int semaphoreMax;

    private final RssRepository rssRepository;
    private final RssArticleTranslatorRepository rssArticleTranslatorRepository;
    private final RssTranslationOrchestration rssTranslationOrchestration;
    private final AiTranslationMapper aiTranslationMapper;
    private final ArticleDedupService articleDedupService;

    @Override
    public OperationResult uploadReport(@Nonnull List<RssNewsSummary> reports, @Nonnull LocalDate reportDate) {
        return rssRepository.uploadReport(reports, reportDate);
    }

    @Override
    public List<RssCategory> listCategories() {
        return rssRepository.getListCategories();
    }

    /**
     * Performs uploading new articles into server
     *
     * @return OperationResult: success/failure with reason
     */
    @Override
    public OperationResult uploadNewArticles(LocalDateTime forWhichDate) {
        OperationResult categoryUploading = uploadCategories();
        if (categoryUploading != null) {
            return categoryUploading;
        }

        OperationResult uploadFeedResult = uploadFeeds();
        if (uploadFeedResult != null) {
            return uploadFeedResult;
        }

        return uploadArticles(forWhichDate);
    }

    /**
     * Uploads articles
     *
     * @return operation result if exists, null otherwise
     */
    private OperationResult uploadArticles(LocalDateTime forWhichDate) {
        List<RssData> listData = rssRepository.getArticlesBy(forWhichDate);

        if (!CollectionUtils.isEmpty(listData)) {
            OperationResult resultUploading = rssRepository.uploadArticles(listData, forWhichDate);
            if (resultUploading.isFailed()) {
                return resultUploading;
            }

            return OperationResult.builder().state(OperationResultEnum.SUCCESS)
                    .reason(String.format("Uploaded %s news to the server for date = %s", listData.size(), forWhichDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                    .build();
        } else {
            return OperationResult.builder().state(OperationResultEnum.SUCCESS).reason(
                    String.format("Nothing to upload into server for date %s", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            ).build();
        }
    }

    /**
     * Uploads feed
     *
     * @return Operation result if exists, null otherwise
     */
    private OperationResult uploadFeeds() {
        List<RssFeed> feedEntities = rssRepository.getListFeeds();

        if (!CollectionUtils.isEmpty(feedEntities)) {
            // Something broken while upload categories
            OperationResult resultUploading = rssRepository.uploadFeeds(feedEntities);
            if (resultUploading.isFailed()) {
                return resultUploading;
            }
        }

        return null;
    }

    /**
     * Uploads categories
     *
     * @return Operation result if exists, null otherwise
     */
    private OperationResult uploadCategories() {
        List<RssCategory> categoryEntities = rssRepository.getListCategories();
        if (!CollectionUtils.isEmpty(categoryEntities)) {
            // Something broken while upload categories
            OperationResult resultUploading = rssRepository.uploadCategories(categoryEntities);
            if (resultUploading.isFailed()) {
                return resultUploading;
            }
        }

        return null;
    }

    @Override
    public OperationResult syncTranslation() {
        List<RssData> rssTranslationList = rssArticleTranslatorRepository.getNotTranslatedNews();

        log.info("Need translate {} articles", rssTranslationList.size());

        List<RssData> articlesToProcess = deduplicateWithinCategories(rssTranslationList);

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore semaphore = new Semaphore(semaphoreMax);

            List<Future<OperationResult>> listFutures = new ArrayList<>();
            for (RssData item : articlesToProcess) {
                listFutures.add(
                        executorService.submit(() -> {
                            semaphore.acquire();

                            try {
                                if (item.getTranslationStatusEnum() == null) {
                                    return rssTranslationOrchestration.initTranslation(item);
                                } else {
                                    if (item.getTranslationStatusEnum() == INIT || item.getTranslationStatusEnum() == FAILURE) {
                                        AiCleaningRequest relevanceRequest = aiTranslationMapper.convert(item);
                                        rssTranslationOrchestration.checkRelevance(relevanceRequest);

                                        return OperationResult.builder().reason("Success").state(OperationResultEnum.SUCCESS).build();
                                    }

                                    log.warn("No handling for the ID = {}. Status is not an INIT or null", item.getArticleId());
                                    return OperationResult.builder().state(OperationResultEnum.FAILURE).reason("Ignore").build();
                                }
                            } finally {
                                semaphore.release();
                            }
                        }));
            }

            for (Future<OperationResult> response : listFutures) {
                response.get();
            }

            return OperationResult.builder().state(OperationResultEnum.SUCCESS)
                    .reason(String.format(
                            "Translation has completed successfully. Count records = %s (duplicates skipped: %s)",
                            articlesToProcess.size(), rssTranslationList.size() - articlesToProcess.size()))
                    .build();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deduplicates articles within each category.
     * Duplicates are immediately marked as SKIPPED in the database.
     *
     * @return list of unique articles that should proceed to translation
     */
    private List<RssData> deduplicateWithinCategories(List<RssData> articles) {
        Map<String, List<RssData>> byCategory = articles.stream()
                .filter(a -> a.getRssCategory() != null)
                .collect(Collectors.groupingBy(a -> a.getRssCategory().getCategoryNameEn()));

        List<RssData> result = new ArrayList<>();
        List<Long> duplicateIds = new ArrayList<>();

        for (List<RssData> categoryArticles : byCategory.values()) {
            if (categoryArticles.size() <= 1) {
                result.addAll(categoryArticles);
                continue;
            }

            List<String> seenHashes = new ArrayList<>();
            List<String> seenTitles = new ArrayList<>();
            List<String> seenContents = new ArrayList<>();

            for (RssData article : categoryArticles) {
                String hash = article.getHash() != null ? article.getHash() : "";
                String title = article.getArticleTitleEn() != null ? article.getArticleTitleEn() : "";
                String content = article.getArticleContent() != null ? article.getArticleContent() : "";

                // Exact match by pre-computed hash
                if (!hash.isEmpty() && seenHashes.contains(hash)) {
                    duplicateIds.add(article.getArticleId());
                    continue;
                }

                boolean isDup = false;
                for (int i = 0; i < seenTitles.size(); i++) {
                    if (articleDedupService.isSimilar(title, content, seenTitles.get(i), seenContents.get(i))) {
                        isDup = true;
                        break;
                    }
                }

                if (isDup) {
                    duplicateIds.add(article.getArticleId());
                    continue;
                }

                if (!hash.isEmpty()) seenHashes.add(hash);
                seenTitles.add(title);
                seenContents.add(content);
                result.add(article);
            }
        }

        // Articles without category bypass dedup
        result.addAll(articles.stream()
                .filter(a -> a.getRssCategory() == null)
                .toList());

        if (!duplicateIds.isEmpty()) {
            rssArticleTranslatorRepository.queryUpdateStatusByListData(duplicateIds, SKIPPED, "Duplicate within category");
            log.info("Dedup: marked {} articles as SKIPPED", duplicateIds.size());
        }

        return result;
    }

    @Override
    public void queryUpdateArticleTranslation(List<AiTranslationResponse> responses,
                                              ArticleTranslationStatusEnum statusEnum, String reason) {
        if (CollectionUtils.isEmpty(responses) || statusEnum == null) {
            throw new IllegalArgumentException("Arguments must be not null");
        }

        rssArticleTranslatorRepository.queryUpdateArticleTranslation(responses, statusEnum, reason);
    }

    @Override
    public void queryUpdateStatusByListData(List<Long> idList, ArticleTranslationStatusEnum statusEnum) {
        if (CollectionUtils.isEmpty(idList) || statusEnum == null) {
            throw new IllegalArgumentException("Arguments must be not null");
        }

        rssArticleTranslatorRepository.queryUpdateStatusByListData(idList, statusEnum);
    }

    @Override
    public void queryUpdateStatusByListData(List<Long> idList, ArticleTranslationStatusEnum statusEnum, String reason) {
        if (CollectionUtils.isEmpty(idList) || statusEnum == null) {
            throw new IllegalArgumentException("Arguments must be not null");
        }

        rssArticleTranslatorRepository.queryUpdateStatusByListData(idList, statusEnum, reason);
    }

    @Override
    public void insertNewArticleTranslations(List<RssData> rssDataList, ArticleTranslationStatusEnum statusEnum) {
        if (CollectionUtils.isEmpty(rssDataList) || statusEnum == null) {
            throw new IllegalArgumentException("Arguments must be not null");
        }

        // Only persist not created translations
        List<RssData> notTranslatedNews = rssDataList.stream().filter(RssData::isNotTranslationExists).toList();
        rssArticleTranslatorRepository.insertNewArticleTranslations(notTranslatedNews, statusEnum);
    }
}
