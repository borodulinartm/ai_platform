package com.huawei.ai_platform.rss.infrastructure.cloud.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssNewsSummaryCloud;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        String reportDateFormatted = reportDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        Path entryPath = Paths.get(basicPath + File.separator + newsSummaryPath + File.separator + reportDateFormatted);

        OperationResult beforeWriting = cloudSender.deleteItems(entryPath);
        if (beforeWriting.isFailed()) {
            return beforeWriting;
        }

        Map<Integer, List<RssNewsSummaryCloud>> mapByCategoryId = summaryClouds.stream()
                .collect(Collectors.groupingBy(RssNewsSummaryCloud::getCategoryId));

        for (Map.Entry<Integer, List<RssNewsSummaryCloud>> item : mapByCategoryId.entrySet()) {
            String path = basicPath + File.separator + newsSummaryPath + File.separator + reportDateFormatted +
                    File.separator + item.getKey() + File.separator;
            try {
                String jsonRepresentation = objectMapper.writeValueAsString(item.getValue());
                cloudSender.upload(path, jsonRepresentation, fileName);
            } catch (JsonProcessingException e) {
                return OperationResult.builder().state(OperationResultEnum.FAILURE).reason("IO error: " + e.getMessage()).build();
            }
        }

        return OperationResult.builder().state(OperationResultEnum.SUCCESS)
                .reason("Operation has completed successfully")
                .build();
    }
}
