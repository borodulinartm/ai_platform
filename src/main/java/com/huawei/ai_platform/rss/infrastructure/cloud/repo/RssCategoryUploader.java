package com.huawei.ai_platform.rss.infrastructure.cloud.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssCategoryCloud;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Category uploader to the Cloud
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Component
@RequiredArgsConstructor
public class RssCategoryUploader {
    private final ObjectMapper objectMapper;
    private final CloudSender cloudSender;

    @Value("${cloud.base-path-files}")
    private String basePathFiles;

    @Value("${cloud.directories.category}")
    private String categoryPath;

    @Value("${cloud.file-name}")
    private String fileName;

    /**
     * Method uploads data to cloud service
     *
     * @param categoryClouds category clouds arr
     * @return OperationResult: success/failure
     */
    public OperationResult uploadRssCategory(@Nonnull List<RssCategoryCloud> categoryClouds) {
        String path = basePathFiles + "/" + categoryPath + "/";
        try {
            String content = objectMapper.writeValueAsString(categoryClouds);
            return cloudSender.upload(path, content, fileName);

        } catch (JsonProcessingException e) {
            return OperationResult.builder().state(OperationResultEnum.FAILURE).reason("Exception: " + e.getMessage()).build();
        }
    }
}
