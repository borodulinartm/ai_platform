package com.huawei.ai_platform.rss.infrastructure.persistence.assembler;

import com.huawei.ai_platform.rss.enums.RssTypeInfoEnum;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFeedEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFetchData;
import com.huawei.ai_platform.rss.model.RssCategory;
import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.rss.model.RssFeed;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.huawei.ai_platform.common.Constant.ZONE;

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
    public static final String REGEX = "\\|";

    /**
     * Performs converting from the {@code RssFetchData} to the {@code RssData}
     *
     * @return List of the rss data
     * @see RssData
     * @see RssFetchData
     *
     */
    public List<RssData> convertFromFetchToRssData(@Nonnull List<RssFetchData> inputData) {
        List<RssData> resultList = new ArrayList<>();

        for (RssFetchData inputItem : inputData) {
            List<String> authorList = StringUtils.isNoneBlank(inputItem.getAuthor())
                    ? Stream.of(inputItem.getAuthor().split(AUTHOR_SEPARATOR)).filter(StringUtils::isNoneBlank)
                        .map(String::trim).toList()
                    : List.of();
            List<String> tagsList = StringUtils.isNoneBlank(inputItem.getTags())
                    ? Stream.of(inputItem.getTags().split(TAG_SEPARATOR))
                    .filter(StringUtils::isNoneBlank).map(String::trim).map(String::trim).distinct().toList()
                    : List.of();

            RssFeed feed = RssFeed.builder()
                    .feedId(inputItem.getFeedId())
                    .description(inputItem.getFeedDescription())
                    .feedNameEn(inputItem.getFeedName()).url(inputItem.getFeedUrl())
                    .website(inputItem.getFeedWebsite()).priority(inputItem.getFeedPriority())
                    .build();
            RssCategory category = RssCategory.builder()
                    .categoryId(inputItem.getCategoryId()).categoryNameEn(inputItem.getCategoryName())
                    .build();

            RssData data = RssData.builder()
                    .articleId(inputItem.getId())
                    .articleTitleEn(inputItem.getTitle()).articleTitleZh(inputItem.getTitleZh())
                    .typeInfoEnum(RssTypeInfoEnum.ARTICLES)
                    .articleContent(inputItem.getContent()).articleContentZh(inputItem.getContentZh())
                    .articleLink(inputItem.getLink())
                    .createDate(Instant.ofEpochSecond(inputItem.getDate()).atZone(ZONE).toLocalDateTime())
                    .articleAuthors(authorList).articleTags(tagsList).feed(feed).rssCategory(category)
                    .build();
            resultList.add(data);
        }

        return resultList;
    }

    /**
     * Performs converting from the persistence side to the RSS category normal
     *
     * @param categoryEntities list of categories
     * @return list of converted data
     */
    public List<RssCategory> convertFromPersistenceToRssCategory(@Nonnull List<RssCategoryEntity> categoryEntities) {
        List<RssCategory> result = new ArrayList<>();

        for (RssCategoryEntity categoryEntity : categoryEntities) {
            String[] categoryArr = categoryEntity.getName().split(REGEX);
            result.add(
                    RssCategory.builder()
                            .categoryId(categoryEntity.getId())
                            .categoryNameEn(categoryArr[0].trim())
                            .categoryNameZh(categoryArr.length > 1 ? categoryArr[1].trim() : StringUtils.EMPTY)
                            .build()
            );
        }

        return result;
    }

    /**
     * Performs converting from the persistence side to the RSS category normal
     *
     * @param rssFeedEntities list of categories
     * @return list of converted data
     */
    public List<RssFeed> convertFromPersistenceToRssFeed(@Nonnull List<RssFeedEntity> rssFeedEntities) {
        List<RssFeed> result = new ArrayList<>();

        for (RssFeedEntity feedEntity : rssFeedEntities) {
            String[] feedArr = feedEntity.getName().split(REGEX);
            result.add(
                    RssFeed.builder()
                            .feedId(feedEntity.getId())
                            .feedNameEn(feedArr[0].trim())
                            .feedNameZh(feedArr.length > 1 ? feedArr[1].trim() : StringUtils.EMPTY)
                            .description(feedEntity.getDescription())
                            .priority(feedEntity.getPriority())
                            .categoryId(feedEntity.getCategoryId())
                            .url(feedEntity.getUrl())
                            .website(feedEntity.getWebsite())
                            .priority(feedEntity.getPriority())
                            .build()
            );
        }

        return result;
    }
}
