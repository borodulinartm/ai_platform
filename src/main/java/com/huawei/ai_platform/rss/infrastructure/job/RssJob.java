package com.huawei.ai_platform.rss.infrastructure.job;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import com.huawei.ai_platform.rss.application.service.RssTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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

    /**
     * Job which runs updating
     */
    @Scheduled(cron = "* * 1 * * ?", zone = "GMT")
    public void runScheduler() {
        log.info("Run RssUploaderJob");

        long previousDays = 7L;
        for (long i = 1L; i <= previousDays; ++i) {
            OperationResult result = rssService.uploadNewArticles(LocalDateTime.now().minusDays(i));
            log.atLevel(result.getState().getLogLevel()).log(result.getInfo());
        }

        log.info("Finish RssUploaderJob");
    }

//    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    public void runTranslation() {
        log.info("Run Translation Job");

        OperationResult result = rssTranslationService.syncTranslation();
        log.atLevel(result.getState().getLogLevel()).log(result.getInfo());

        log.info("Finish Translation Job");
    }
}
