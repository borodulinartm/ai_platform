package com.huawei.ai_platform.rss.infrastructure.job;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rss uploader job
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RssUploaderJob {
    private final RssSyncService rssService;

    /**
     * Job which runs updating
     */
    @Scheduled(cron = "* * 0 * * ?", zone = "GMT")
    public void runScheduler() {
        log.info("Run RssUploaderJob");

        OperationResult result = rssService.uploadNewArticles();
        log.atLevel(result.getState().getLogLevel()).log(result.getInfo());

        log.info("Finish RssUploaderJob");
    }

}
