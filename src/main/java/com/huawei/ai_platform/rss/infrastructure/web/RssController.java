package com.huawei.ai_platform.rss.infrastructure.web;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.rss.application.service.RssConfigService;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import com.huawei.ai_platform.rss.infrastructure.web.assembler.RssNewsAssembler;
import com.huawei.ai_platform.rss.infrastructure.web.model.RssReportDto;
import com.huawei.ai_platform.rss.model.RssCategory;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Web input layer
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/upload-report")
@Slf4j
public class RssController {
    private final RssConfigService rssConfigService;
    private final RssSyncService rssSyncService;
    private final RssNewsAssembler rssNewsAssembler;

    /**
     * Uploads news to the cloud
     *
     * @param reportDate for which date do you want upload info?
     * @param body       report info JSON
     */
    @PostMapping
    public ResponseEntity<?> uploadReportToServer(
            @RequestParam(name = "report_date") LocalDate reportDate,
            @RequestBody List<RssReportDto> body
    ) {
        Map<Integer, RssCategory> categoryEntityList = rssConfigService.listCategories()
                .stream().collect(Collectors.toMap(RssCategory::getCategoryId, Function.identity(), (a, b) -> a));
        List<RssNewsSummary> newsSummaries = body.stream()
                .map(v -> rssNewsAssembler.toAggregate(v, categoryEntityList)).toList();

        OperationResult result = rssSyncService.uploadReport(newsSummaries, reportDate);
        if (result.isFailed()) {
            return ResponseEntity.internalServerError().body(result.getInfo());
        } else {
            return ResponseEntity.ok().build();
        }
    }
}
