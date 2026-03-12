package com.huawei.ai_platform.rss.infrastructure.cloud.repo;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

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

            Path fullPath;
            if (!path.endsWith(File.separator)) {
                fullPath = Paths.get(path + File.separator + fileName);
            } else {
                fullPath = Paths.get(path + fileName);
            }

            OutputStream stream = Files.newOutputStream(fullPath);
            stream.write(text.getBytes(StandardCharsets.UTF_8));
            stream.close();

            return OperationResult.builder().state(OperationResultEnum.SUCCESS).reason("Successfully uploaded").build();
        } catch (IOException e) {
            return OperationResult.builder().state(OperationResultEnum.FAILURE).reason(e.getMessage()).build();
        }
    }

    /**
     * Performs low-level removing items
     *
     * @param paths collection of path items
     * @return OperationResult: success/failure
     */
    public OperationResult deleteItems(@Nonnull Collection<Path> paths) {
        return deleteItems(paths, Set.of());
    }

    /**
     * Overloading method for removing files with support of an excluding not useful files
     *
     * @param paths   collection of files
     * @param exclude collection of excluded files
     * @return OperationResult with success/failure
     */
    public OperationResult deleteItems(@Nonnull Collection<Path> paths, Collection<Path> exclude) {
        try {
            for (Path file : paths) {
                if (!exclude.contains(file)) {
                    Files.delete(file);
                }
            }

            return OperationResult.builder().state(OperationResultEnum.SUCCESS).reason("Successfully deleted").build();
        } catch (IOException exception) {
            return OperationResult.builder().state(OperationResultEnum.FAILURE).reason("IO error: " + exception.getMessage()).build();
        }
    }
}
