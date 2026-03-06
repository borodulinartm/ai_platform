package com.huawei.freshrss_news_upload.rss.application.service;

import com.huawei.freshrss_news_upload.rss.infrastructure.RssFacade;
import com.huawei.freshrss_news_upload.rss.model.RssData;
import com.huawei.freshrss_news_upload.utils.OperationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.huawei.freshrss_news_upload.utils.OperationResultEnum.SUCCESS;

/**
 * Business logic layer
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RssService {
    private final RssFacade rssRepository;

    /**
     * Performs uploading new articles into server
     *
     * @return OperationResult: success/failure with reason
     */
    public OperationResult uploadNewArticles() {
        List<RssData> listData = rssRepository.getUnreadRssDataBy(LocalDateTime.now().minusDays(1L));

        if (!CollectionUtils.isEmpty(listData)) {
            for (RssData article : listData) {
                // SEND TO FS ACCORDING TO MOUNT POINT
            }

            return OperationResult.builder().state(SUCCESS).reason("The operation has completed successfully").build();
        } else {
            return OperationResult.builder().state(SUCCESS).reason(
                        String.format("Nothing to upload into server for date %s", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    ).build();
        }
    }
}
