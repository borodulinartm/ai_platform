package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.repo.RssRepository;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFeedEntity;
import com.huawei.ai_platform.rss.model.RssData;
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
public class RssService {
    private final RssRepository rssRepository;

    /**
     * Performs uploading new articles into server
     *
     * @return OperationResult: success/failure with reason
     */
    public OperationResult uploadNewArticles() {
        OperationResult categoryUploading = uploadCategories();
        if (categoryUploading != null) {
            return categoryUploading;
        }

        OperationResult uploadFeedResult = uploadFeeds();
        if (uploadFeedResult != null) {
            return uploadFeedResult;
        }

        return uploadArticles();
    }

    /**
     * Uploads articles
     *
     * @return operation result if exists, null otherwise
     */
    private OperationResult uploadArticles() {
        LocalDateTime articlesDateTime = LocalDateTime.now().minusDays(1L);
        List<RssData> listData = rssRepository.getArticlesBy(articlesDateTime);

        if (!CollectionUtils.isEmpty(listData)) {
            OperationResult resultUploading = rssRepository.uploadArticles(listData, articlesDateTime);
            if (resultUploading.isFailed()) {
                return resultUploading;
            }

            return OperationResult.builder().state(OperationResultEnum.SUCCESS).reason(String.format("Uploaded %s news to the server", listData.size())).build();
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
        List<RssFeedEntity> feedEntities = rssRepository.getListFeeds();
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
        List<RssCategoryEntity> categoryEntities = rssRepository.getListCategories();
        if (!CollectionUtils.isEmpty(categoryEntities)) {
            // Something broken while upload categories
            OperationResult resultUploading = rssRepository.uploadCategories(categoryEntities);
            if (resultUploading.isFailed()) {
                return resultUploading;
            }
        }

        return null;
    }
}
