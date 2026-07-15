package com.huawei.ai_platform.rss.application.service.impl;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssNewsSummaryCloud;
import com.huawei.ai_platform.rss.application.repo.RssRepository;
import com.huawei.ai_platform.rss.application.service.RssTopArticlesService;
import com.huawei.ai_platform.rss.infrastructure.ai.executor.AiTopArticlesOrchestrator;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;

import static com.huawei.ai_platform.common.Constant.ZONE;

@Service
@Slf4j
public class RssTopArticlesServiceImpl implements RssTopArticlesService {

    private final AiTopArticlesOrchestrator aiOrchestrator;
    private final RssRepository rssRepository;
    private final int topArticlesCount;

    public RssTopArticlesServiceImpl(
            AiTopArticlesOrchestrator aiOrchestrator,
            RssRepository rssRepository,
            @Value("${ai.digest.top-articles-count:20}") int topArticlesCount) {
        this.aiOrchestrator = aiOrchestrator;
        this.rssRepository = rssRepository;
        this.topArticlesCount = topArticlesCount;
    }

    @Override
    public OperationResult processTopArticles(@Nonnull LocalDate forWhichDate) {
        log.info("Starting TOP-{} articles processing for date: {}", topArticlesCount, forWhichDate);

        long startTime = forWhichDate.atStartOfDay(ZONE).toEpochSecond();
        long endTime = forWhichDate.atTime(23, 59, 59).atZone(ZONE).toEpochSecond();

        List<RssNewsSummaryCloud> summaries = aiOrchestrator.processArticles(startTime, endTime);

        if (CollectionUtils.isEmpty(summaries)) {
            return OperationResult.builder()
                    .state(OperationResultEnum.SUCCESS)
                    .reason("No articles processed")
                    .build();
        }

        OperationResult uploadResult = rssRepository.uploadCloudReport(summaries, forWhichDate);

        if (uploadResult.isFailed()) {
            return uploadResult;
        }

        log.info("Successfully processed {} categories", summaries.size());

        return OperationResult.builder()
                .state(OperationResultEnum.SUCCESS)
                .reason(String.format("Processed %d categories with TOP-%d articles", summaries.size(), topArticlesCount))
                .build();
    }
}
