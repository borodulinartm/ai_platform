package com.huawei.ai_platform.rss.infrastructure.cloud.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssFeedCloud;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Feed uploader to the Cloud
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Component
@RequiredArgsConstructor
public class RssFeedUploader {
    private static final String FOLDER_CATEGORY = "feed";
    private static final String FILE_NAME = "feeds";

    private final ObjectMapper objectMapper;
    private final CloudSender cloudSender;

    @Value("${app.base-path-files}")
    private String basePathFiles;

    /**
     * Method uploads data to cloud service
     *
     * @param rssFeedCloudList category clouds arr
     */
    public OperationResult uploadRssCategory(@Nonnull Collection<RssFeedCloud> rssFeedCloudList) {
        String path = basePathFiles + "/" + FOLDER_CATEGORY + "/";

        try {
            String content = objectMapper.writeValueAsString(rssFeedCloudList);
            return cloudSender.upload(path, content, FILE_NAME);

        } catch (JsonProcessingException e) {
            return OperationResult.builder().state(OperationResultEnum.FAILURE).reason("Exception: " + e.getMessage()).build();
        }
    }
}
