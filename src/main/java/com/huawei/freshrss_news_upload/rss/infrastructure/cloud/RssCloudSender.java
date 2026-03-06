package com.huawei.freshrss_news_upload.rss.infrastructure.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.freshrss_news_upload.common.OperationResult;
import com.huawei.freshrss_news_upload.rss.infrastructure.cloud.model.CategoryFeeKey;
import com.huawei.freshrss_news_upload.rss.model.RssData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.huawei.freshrss_news_upload.common.OperationResultEnum.FAILURE;
import static com.huawei.freshrss_news_upload.common.OperationResultEnum.SUCCESS;

/**
 * Component that uploads to HUAWEI Cloud
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssCloudSender {
    private final ObjectMapper objectMapper;

    @Value("${app.base-path-files}")
    private String basePathFiles;

    /**
     * Performs uploading to Huawei Cloud service
     * It uses mounted directory from the Huawei Cloud service
     *
     * @param data item to send
     * @return OperationResult with status: success/failure
     */
    public OperationResult upload(Collection<RssData> data, LocalDateTime reportDate) {
        if (data == null) {
            return OperationResult.builder().state(FAILURE).reason("Data is null").build();
        }

        if (reportDate == null) {
            return OperationResult.builder().state(FAILURE).reason("Report date is null").build();
        }

        OperationResult validationResult = checkBeforeSave(data);
        if (validationResult.isFailed()) {
            return validationResult;
        }

        Map<CategoryFeeKey, List<RssData>> rssMap = data.stream()
                .collect(
                        Collectors.groupingBy(v -> CategoryFeeKey.of(v.getCategory().getCategoryId(),
                                v.getFeed().getFeedId()))
                );

        for (Map.Entry<CategoryFeeKey, List<RssData>> entryItem : rssMap.entrySet()) {
            String pathInCloud = basePathFiles + "news" + File.separator +
                    reportDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) + File.separator +
                    entryItem.getKey().getCategoryId() + File.separator + entryItem.getKey().getFeedId() + File.separator;
            try {
                Files.createDirectories(Path.of(pathInCloud));

                OutputStream stream = Files.newOutputStream(Path.of(pathInCloud + "report"));
                stream.write(objectMapper.writeValueAsString(entryItem.getValue()).getBytes(StandardCharsets.UTF_8));
                stream.close();
            } catch (IOException e) {
                log.error("Output stream error");
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
    private OperationResult checkBeforeSave(Collection<RssData> rssDataCollection) {
        for (RssData data : rssDataCollection) {
            if (data.getCategory() == null) {
                return OperationResult.builder().state(FAILURE).reason("Category is null").build();
            }

            if (data.getFeed() == null) {
                return OperationResult.builder().state(FAILURE).reason("Feed is null").build();
            }
        }

        return OperationResult.builder().state(SUCCESS).reason("").build();
    }
}
