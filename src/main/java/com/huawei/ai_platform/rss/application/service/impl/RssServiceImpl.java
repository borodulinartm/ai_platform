package com.huawei.ai_platform.rss.application.service.impl;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.repo.RssArticleTranslatorRepository;
import com.huawei.ai_platform.rss.application.repo.RssRepository;
import com.huawei.ai_platform.rss.application.service.RssConfigService;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import com.huawei.ai_platform.rss.application.service.RssTranslationService;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationResponse;
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
import java.util.List;

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
    private final RssRepository rssRepository;
    private final RssArticleTranslatorRepository rssArticleTranslatorRepository;

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
        return rssArticleTranslatorRepository.syncTranslation(rssTranslationList);
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

        rssArticleTranslatorRepository.insertNewArticleTranslations(rssDataList, statusEnum);
    }
}
