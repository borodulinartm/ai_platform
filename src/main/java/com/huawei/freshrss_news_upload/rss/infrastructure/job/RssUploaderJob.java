package com.huawei.freshrss_news_upload.rss.infrastructure.job;

import com.huawei.freshrss_news_upload.rss.application.service.RssService;
import com.huawei.freshrss_news_upload.utils.OperationResult;
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
    private final RssService rssService;

    @Scheduled(cron = "0 0 6 * * ?")
    public void runScheduler() {
        log.info("Run RssUploaderJob");

        OperationResult result = rssService.uploadNewArticles();
        log.atLevel(result.getState().getLogLevel()).log(result.getInfo());

        log.info("Finish RssUploaderJob");
    }
}
