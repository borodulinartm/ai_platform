package com.huawei.ai_platform.rss.infrastructure.cloud.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.CategoryFeeKey;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssArticleCloud;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.huawei.ai_platform.common.OperationResultEnum.FAILURE;
import static com.huawei.ai_platform.common.OperationResultEnum.SUCCESS;
import static com.huawei.ai_platform.rss.enums.RssTypeInfoEnum.ARTICLES;

/**
 * Rss articles uploader repo layer
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RssArticlesUploader {
    public static final String FILE_NAME = "report";

    private final ObjectMapper objectMapper;
    private final CloudSender cloudSender;

    @Value("${app.base-path-files}")
    private String basePathFiles;

    /**
     * Uploads data to cloud
     *
     * @param dataItems  list of the article clouds
     * @param reportDate date of creating new fields
     * @return OperationResult: success if ok, failed otherwise
     */
    public OperationResult upload(@Nonnull List<RssArticleCloud> dataItems, @Nonnull LocalDateTime reportDate) {
        OperationResult validationResult = checkBeforeSave(dataItems);

        if (validationResult.isFailed()) {
            return validationResult;
        }

        OperationResult removingOldData = deleteOldData(reportDate);
        if (removingOldData.isFailed()) {
            return removingOldData;
        }

        Map<CategoryFeeKey, List<RssArticleCloud>> rssMap = dataItems.stream()
                .collect(
                        Collectors.groupingBy(v -> CategoryFeeKey.of(v.getCategoryId(),
                                v.getFeedId(), ARTICLES))
                );

        for (Map.Entry<CategoryFeeKey, List<RssArticleCloud>> entryItem : rssMap.entrySet()) {
            String pathInCloud = basePathFiles + entryItem.getKey().getTypeInfoEnum().name().toLowerCase(Locale.ENGLISH) + File.separator +
                    reportDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) + File.separator +
                    entryItem.getKey().getCategoryId() + File.separator + entryItem.getKey().getFeedId() + File.separator;
            try {
                cloudSender.upload(pathInCloud, objectMapper.writeValueAsString(entryItem.getValue()), FILE_NAME);
            } catch (JsonProcessingException e) {
                return OperationResult.builder().state(FAILURE).reason("Bad situation. " + e.getMessage()).build();
            }
        }

        return OperationResult.builder().state(SUCCESS).reason("Operation has completed successfully").build();
    }

    /**
     * Performs validation before saving it to the cloud
     *
     * @param rssDataCollection collection of the RSS data collection
     * @return OperationResult: success/failure
     */
    private OperationResult checkBeforeSave(Collection<RssArticleCloud> rssDataCollection) {
        for (RssArticleCloud data : rssDataCollection) {
            if (data.getCategoryId() == 0) {
                return OperationResult.builder().state(FAILURE).reason("Category is null").build();
            }

            if (data.getFeedId() == 0) {
                return OperationResult.builder().state(FAILURE).reason("Feed is null").build();
            }
        }

        return OperationResult.builder().state(SUCCESS).reason("").build();
    }

    /**
     * Performs removing old data
     *
     * @param reportDate report date
     * @return OperationResult: success/failure.
     */
    private OperationResult deleteOldData(LocalDateTime reportDate) {
        Path entryPath = Paths.get(basePathFiles + File.separator + ARTICLES.name().toLowerCase(Locale.ENGLISH)
                + File.separator + reportDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"))
        );

        return cloudSender.deleteItems(entryPath);
    }
}
