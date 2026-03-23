package com.huawei.ai_platform.rss.infrastructure.job;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import com.huawei.ai_platform.rss.application.service.RssTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    /**
     * Job which runs updating
     */
    @Scheduled(cron = "* * 0 * * ?", zone = "GMT")
    public void runScheduler() {
        log.info("Run RssUploaderJob");

        OperationResult result = rssService.uploadNewArticles(LocalDateTime.now().minusDays(1L));
        log.atLevel(result.getState().getLogLevel()).log(result.getInfo());

        log.info("Finish RssUploaderJob");
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void runTranslation() {
        log.info("Run Translation Job");

        OperationResult result = rssTranslationService.performTranslate();
        log.atLevel(result.getState().getLogLevel()).log(result.getInfo());

        log.info("Finish Translation Job");
    }
}
