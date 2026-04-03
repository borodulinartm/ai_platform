package com.huawei.ai_platform.rss.application.service.impl;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.repo.RssRepository;
import com.huawei.ai_platform.rss.application.service.RssTopArticlesService;
import com.huawei.ai_platform.rss.infrastructure.ai.repo.AiTopArticlesOrchestrator;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;

import static com.huawei.ai_platform.common.Constant.ZONE;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssTopArticlesServiceImpl implements RssTopArticlesService {

    private final AiTopArticlesOrchestrator aiOrchestrator;
    private final RssRepository rssRepository;

    @Override
    public OperationResult processTopArticles(@Nonnull LocalDate forWhichDate) {
        log.info("Starting TOP-10 articles processing for date: {}", forWhichDate);

        long startTime = forWhichDate.atStartOfDay(ZONE).toEpochSecond();
        long endTime = forWhichDate.atTime(23, 59, 59).atZone(ZONE).toEpochSecond();

        List<RssNewsSummary> summaries = aiOrchestrator.processArticles(startTime, endTime);

        if (CollectionUtils.isEmpty(summaries)) {
            return OperationResult.builder()
                    .state(OperationResultEnum.SUCCESS)
                    .reason("No articles processed")
                    .build();
        }

        OperationResult uploadResult = rssRepository.uploadReport(summaries, forWhichDate);

        if (uploadResult.isFailed()) {
            return uploadResult;
        }

        log.info("Successfully processed {} categories", summaries.size());

        return OperationResult.builder()
                .state(OperationResultEnum.SUCCESS)
                .reason(String.format("Processed %d categories with TOP-10 articles", summaries.size()))
                .build();
    }
}
