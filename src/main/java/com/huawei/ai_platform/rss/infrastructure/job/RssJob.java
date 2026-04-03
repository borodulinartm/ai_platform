package com.huawei.ai_platform.rss.infrastructure.job;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.annotation.DbLock;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import com.huawei.ai_platform.rss.application.service.RssTopArticlesService;
import com.huawei.ai_platform.rss.application.service.RssTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rss different job
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RssJob {
    private final RssSyncService rssService;
    private final RssTranslationService rssTranslationService;
    private final RssTopArticlesService rssTopArticlesService;

    /**
     * Job for uploading into data storage components of our RSS
     * Just in case we upload data for {@code previousDays} several days ago
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = "GMT")
    @DbLock(category = "articles_uploading_lock")
    public void runUploadingToCloud() {
        log.info("Run Rss uploading to the Huawei Cloud");

        long previousDays = 1L;
        for (long i = 1L; i <= previousDays; ++i) {
            OperationResult result = rssService.uploadNewArticles(LocalDateTime.now().minusDays(i));
            log.atLevel(result.getState().getLogLevel()).log(result.getInfo());
        }

        log.info("Finish Rss uploading to the Huawei Cloud");
    }

    /**
     * Job for adding translations for newest articles or scientific papers
     */
    @Scheduled(cron = "0 15 * * * ?", zone = "GMT")
    @DbLock(category = "articles_translating_lock")
    public void runTranslation() {
        log.info("Run Translation Job");

        OperationResult result = rssTranslationService.syncTranslation();
        log.atLevel(result.getState().getLogLevel()).log(result.getInfo());

        log.info("Finish Translation Job");
    }

    /**
     * Job for processing TOP-10 articles per category with summaries
     * Runs daily at 2:00 AM GMT
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "GMT")
    @DbLock(category = "top_articles_processing_lock")
    public void runTopArticlesProcessing() {
        log.info("Run TOP-10 Articles Processing Job");

        OperationResult result = rssTopArticlesService.processTopArticles(LocalDate.now().minusDays(1));
        log.atLevel(result.getState().getLogLevel()).log(result.getInfo());

        log.info("Finish TOP-10 Articles Processing Job");
    }
}
