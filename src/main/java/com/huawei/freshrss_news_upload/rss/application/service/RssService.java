package com.huawei.freshrss_news_upload.rss.application.service;

import com.huawei.freshrss_news_upload.rss.application.repo.RssRepository;
import com.huawei.freshrss_news_upload.rss.model.RssData;
import com.huawei.freshrss_news_upload.common.OperationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.huawei.freshrss_news_upload.common.OperationResultEnum.SUCCESS;

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
    private final RssRepository rssRepository;

    /**
     * Performs uploading new articles into server
     *
     * @return OperationResult: success/failure with reason
     */
    public OperationResult uploadNewArticles() {
        LocalDateTime articlesDateTime = LocalDateTime.now().minusDays(1L);
        List<RssData> listData = rssRepository.getUnreadItemsBy(articlesDateTime);

        if (!CollectionUtils.isEmpty(listData)) {
            OperationResult resultUploading = rssRepository.sendToCloud(listData, articlesDateTime);
            if (resultUploading.isFailed()) {
                return resultUploading;
            }

            return OperationResult.builder().state(SUCCESS).reason(String.format("Uploaded %s news to the server", listData.size())).build();
        } else {
            return OperationResult.builder().state(SUCCESS).reason(
                        String.format("Nothing to upload into server for date %s", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    ).build();
        }
    }
}
