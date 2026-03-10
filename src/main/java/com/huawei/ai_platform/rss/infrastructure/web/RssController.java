package com.huawei.ai_platform.rss.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Web input layer
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/upload-report")
public class RssController {
    /**
     * Uploads news to the cloud
     *
     * @param body report info JSON
     */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadReportToServer(@RequestBody String body) {
        throw new UnsupportedOperationException("Currently not available, please wait for updates!");
    }
}
