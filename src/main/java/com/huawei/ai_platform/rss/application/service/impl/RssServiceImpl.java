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
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping.AiScrappingRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import com.huawei.ai_platform.rss.model.RssCategory;
import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.rss.model.RssFeed;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.FAILURE;
import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.INIT;

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
    public static final int COUNT_THREADS = 15;

    private final RssRepository rssRepository;
    private final RssArticleTranslatorRepository rssArticleTranslatorRepository;
    private final RssTranslationOrchestration rssTranslationOrchestration;
    private final AiTranslationMapper aiTranslationMapper;

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

        try (ExecutorService executorService = Executors.newFixedThreadPool(COUNT_THREADS)) {
            List<Future<OperationResult>> listFutures = new ArrayList<>();
            for (RssData item : rssTranslationList) {
                listFutures.add(
                        executorService.submit(() -> {
                            if (item.getTranslationStatusEnum() == null) {
                                return rssTranslationOrchestration.initTranslation(item);
                            } else {
                                if (item.getTranslationStatusEnum() == INIT || item.getTranslationStatusEnum() == FAILURE) {
                                    AiScrappingRequest cleaningRequest = aiTranslationMapper.convert(item);
                                    rssTranslationOrchestration.scrapContent(cleaningRequest);

                                    return OperationResult.builder().reason("Success").state(OperationResultEnum.SUCCESS).build();
                                }

                                log.warn("No handling for the ID = {}. Status is not an INIT or null", item.getArticleId());
                                return OperationResult.builder().state(OperationResultEnum.FAILURE).reason("Ignore").build();
                            }
                        }));
            }

            for (Future<OperationResult> response : listFutures) {
                response.get();
            }

            return OperationResult.builder().state(OperationResultEnum.SUCCESS)
                    .reason(String.format("Translation has completed successfully. Count records = %s", rssTranslationList.size()))
                    .build();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
    public void insertNewArticleTranslations(List<RssData> rssDataList, ArticleTranslationStatusEnum statusEnum) {
        if (CollectionUtils.isEmpty(rssDataList) || statusEnum == null) {
            throw new IllegalArgumentException("Arguments must be not null");
        }

        // Only persist not created translations
        List<RssData> notTranslatedNews = rssDataList.stream().filter(RssData::isNotTranslationExists).toList();
        rssArticleTranslatorRepository.insertNewArticleTranslations(notTranslatedNews, statusEnum);
    }
}
