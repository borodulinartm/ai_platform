package com.huawei.ai_platform.rss.infrastructure.cloud.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssNewsSummaryCloud;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Component which uploads rss reports
 *
 * @author Borodulin Artem
 * @since 2026.03.12
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RssReportUploader {

    @Value("${cloud.base-path-files}")
    private String basicPath;

    @Value("${cloud.directories.news_summary}")
    private String newsSummaryPath;

    @Value("${cloud.file-name}")
    private String fileName;

    private final CloudSender cloudSender;
    private final ObjectMapper objectMapper;

    /**
     * Performs report uploading into cloud
     *
     * @param summaryClouds list of RSS news summary cloud
     * @param reportDate    for which date do you want to make a report
     * @return OperationResult with status: success/failure
     */
    public OperationResult uploadReport(@Nonnull List<RssNewsSummaryCloud> summaryClouds, @Nonnull LocalDate reportDate) {
        log.info("Starting uploadReport with {} summaries for date {}", summaryClouds.size(), reportDate);
        
        String cleanBasicPath = basicPath.replaceAll("[\\u202A-\\u202E]", "");
        String reportDateFormatted = reportDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        Path entryPath = Path.of(cleanBasicPath, newsSummaryPath, reportDateFormatted);
        log.info("Upload path: {}", entryPath);

        OperationResult beforeWriting = cloudSender.deleteItems(entryPath);
        if (beforeWriting.isFailed()) {
            log.error("Failed to delete existing items at {}: {}", entryPath, beforeWriting.getInfo());
            return beforeWriting;
        }
        log.debug("Successfully deleted existing items at {}", entryPath);

        Map<Integer, List<RssNewsSummaryCloud>> mapByCategoryId = summaryClouds.stream()
                .collect(Collectors.groupingBy(RssNewsSummaryCloud::getCategoryId));
        log.info("Grouped summaries into {} categories", mapByCategoryId.size());

        for (Map.Entry<Integer, List<RssNewsSummaryCloud>> item : mapByCategoryId.entrySet()) {
            Path path = Path.of(cleanBasicPath, newsSummaryPath, reportDateFormatted, String.valueOf(item.getKey()));
            log.info("Uploading category {} with {} articles to {}", item.getKey(), item.getValue().size(), path);

            try {
                String jsonRepresentation = objectMapper.writeValueAsString(item.getValue());
                cloudSender.upload(path, jsonRepresentation, fileName);
                log.info("Successfully uploaded category {} ({} bytes)", item.getKey(), jsonRepresentation.length());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize summaries for category {}: {}", item.getKey(), e.getMessage());
                return OperationResult.builder().state(OperationResultEnum.FAILURE).reason("IO error: " + e.getMessage()).build();
            }
        }

        log.info("Upload completed successfully for {} categories", mapByCategoryId.size());
        return OperationResult.builder().state(OperationResultEnum.SUCCESS)
                .reason("Operation has completed successfully")
                .build();
    }
}
