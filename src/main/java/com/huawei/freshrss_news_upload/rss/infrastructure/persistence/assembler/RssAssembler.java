package com.huawei.freshrss_news_upload.rss.infrastructure.persistence.assembler;

import com.huawei.freshrss_news_upload.rss.infrastructure.persistence.entity.RssFetchData;
import com.huawei.freshrss_news_upload.rss.model.RssCategory;
import com.huawei.freshrss_news_upload.rss.model.RssData;
import com.huawei.freshrss_news_upload.rss.model.RssFeed;
import com.huawei.freshrss_news_upload.utils.Constant;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.huawei.freshrss_news_upload.utils.Constant.ZONE;

/**
 * Mapper class for RSS in the persistence section
 *
 * @author Borodulin Artem b60078502
 * @since 2026.03.07
 */
@Component
@RequiredArgsConstructor
public class RssAssembler {
    private static final String TAG_SEPARATOR = "#";
    private static final String AUTHOR_SEPARATOR = ";";

    /**
     * Performs converting from the {@code RssFetchData} to the {@code RssData}
     *
     * @return List of the rss data
     * @see RssData
     * @see RssFetchData
     *
     */
    public List<RssData> convertFromFetchToRssData(List<RssFetchData> inputData) {
        List<RssData> resultList = new ArrayList<>();

        for (RssFetchData inputItem : inputData) {
            List<String> authorList = StringUtils.isNoneBlank(inputItem.getAuthor())
                    ? Stream.of(inputItem.getAuthor().split(AUTHOR_SEPARATOR)).filter(StringUtils::isNoneBlank).toList()
                    : List.of();
            List<String> tagsList = StringUtils.isNoneBlank(inputItem.getTags())
                    ? Stream.of(inputItem.getTags().split(TAG_SEPARATOR))
                        .filter(StringUtils::isNoneBlank).map(String::trim).distinct()
                        .toList()
                    : List.of();

            RssFeed feed = RssFeed.builder()
                    .description(inputItem.getFeedDescription())
                    .name(inputItem.getFeedName()).url(inputItem.getFeedUrl())
                    .website(inputItem.getFeedWebsite()).priority(inputItem.getFeedPriority())
                    .build();
            RssCategory category = RssCategory.builder()
                    .categoryId(inputItem.getCategoryId()).categoryName(inputItem.getCategoryName())
                    .build();

            RssData data = RssData.builder()
                    .itemId(inputItem.getId()).title(inputItem.getTitle())
                    .content(inputItem.getContent()).link(inputItem.getLink())
                    .creationDate(Instant.ofEpochSecond(inputItem.getDate()).atZone(ZONE).toLocalDateTime())
                    .authors(authorList).tags(tagsList).feed(feed).category(category)
                    .build();
            resultList.add(data);
        }

        return resultList;
    }
}
