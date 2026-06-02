package com.huawei.ai_platform.rss.infrastructure.job;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.annotation.DbLock;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import com.huawei.ai_platform.rss.application.service.RssTopArticlesService;
import com.huawei.ai_platform.rss.application.service.RssTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Rss different job
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.scheduling.enabled",
        havingValue = "true"
)
public class RssJob {
    @Value("${cloud.windowSize:1}")
    private long windowSize;

    @Value("${ai.crawl.script-path:src/main/resources/ai-crawl/vibe-main.py}")
    private String crawlScriptPath;

    @Value("${ai.crawl.python-path:python}")
    private String pythonPath;

    @Value("${ai.crawl.enabled:false}")
    private boolean crawlEnabled;

    @Value("${ai.crawl.llm-url:https://openrouter.ai/api/v1}")
    private String llmUrl;

    @Value("${ai.crawl.llm-key:}")
    private String llmKey;

    private final RssSyncService rssService;
    private final RssTranslationService rssTranslationService;
    private final RssTopArticlesService rssTopArticlesService;

    /**
     * Job for uploading into data storage components of our RSS
     * Just in case we upload data for {@code previousDays} several days ago
     * Need uploader more often because the amount of articles is too high and AI cannot finished translate news in time
     */
    @Scheduled(cron = "0 0 * * * ?", zone = "GMT")
    @DbLock(category = "articles_uploading_lock")
    public void runUploadingToCloud() {
        log.info("Run Rss uploading to the Huawei Cloud");

        for (long i = 1L; i <= (windowSize + 1); ++i) {
            OperationResult result = rssService.uploadNewArticles(LocalDateTime.now().minusDays(i));
            log.atLevel(result.getState().getLogLevel()).log(result.getInfo());
        }

        log.info("Finish Rss uploading to the Huawei Cloud");
    }

    /**
     * Job for adding translations for newest articles or scientific papers
     */
    @Scheduled(cron = "0 25 * * * ?", zone = "GMT")
    @DbLock(category = "articles_translating_lock")
    public void runTranslation() {
        log.info("Run Translation Job");

        OperationResult result = rssTranslationService.syncTranslation();
        log.atLevel(result.getState().getLogLevel()).log(result.getInfo());

        log.info("Finish Translation Job");
    }

    /**
     * Job for processing TOP-10 articles per category with summaries
     * Runs daily at 00:01 AM GMT
     */
    @Scheduled(cron = "0 1 0 * * ?", zone = "GMT")
    @DbLock(category = "top_articles_processing_lock")
    public void runTopArticlesProcessing() {
        log.info("Run TOP-10 Articles Processing Job");

        OperationResult result = rssTopArticlesService.processTopArticles(LocalDate.now().minusDays(1));
        log.atLevel(result.getState().getLogLevel()).log(result.getInfo());

        log.info("Finish TOP-10 Articles Processing Job");
    }

    /**
     * Job for crawling scraped:: sources via Python script
     * Runs every hour at :05
     */
    @Scheduled(cron = "0 5 * * * ?", zone = "GMT")
    @DbLock(category = "ai_crawl_lock")
    public void runAiCrawl() {
        if (!crawlEnabled) {
            log.debug("AI crawl job disabled");
            return;
        }

        log.info("Run AI Crawl Job");

        try {
            ProcessBuilder pb = new ProcessBuilder(pythonPath, crawlScriptPath);
            pb.redirectErrorStream(true);
            pb.directory(Path.of(crawlScriptPath).getParent().toFile());
            pb.environment().put("LLM_URL", llmUrl);
            pb.environment().put("LLM_KEY", llmKey);

            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("AI Crawl completed successfully");
                if (!output.isEmpty()) {
                    log.debug("AI Crawl output:\n{}", output);
                }
            } else {
                log.error("AI Crawl failed with exit code {}: {}", exitCode, output);
            }
        } catch (Exception e) {
            log.error("AI Crawl job error", e);
        }

        log.info("Finish AI Crawl Job");
    }
}
