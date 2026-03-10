package com.huawei.ai_platform.rss.infrastructure.cloud.repo;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Low-level cloud sender
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Component
@RequiredArgsConstructor
public class CloudSender {

    /**
     * Uploads to cloud
     *
     * @param path     path
     * @param text     text items
     * @param fileName file name
     * @return OperationResult: success/failure
     */
    public OperationResult upload(String path, String text, String fileName) {
        try {
            Files.createDirectories(Path.of(path));

            OutputStream stream = Files.newOutputStream(Path.of(path + "report"));
            stream.write(text.getBytes(StandardCharsets.UTF_8));
            stream.close();

            return OperationResult.builder().state(OperationResultEnum.SUCCESS).reason("Successfully uploaded").build();
        } catch (IOException e) {
            return OperationResult.builder().state(OperationResultEnum.FAILURE).reason(e.getMessage()).build();
        }
    }
}
