package com.huawei.ai_platform.rss.infrastructure.persistence.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssArticleTranslationEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFetchData;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import jakarta.annotation.Nonnull;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * Lower level (DAO)
 *
 * @author Borodulin Artem b60078502
 * @since 2026.03.07
 */
@Mapper
public interface RssDao extends BaseMapper<RssEntity> {
    /**
     * Extracts list of unread items by date
     *
     * @param start start time
     * @param end   end time
     * @return list of the fetched rss data
     */
    List<RssFetchData> queryArticlesBy(
            @Param("startTime") long start,
            @Param("endTime") long end
    );

    /**
     * Extracts list of articles after some timestamp
     *
     * @param timestamp   timestamp. If is empty, it returns all data
     * @param articleDate article date
     * @return List of fetched data
     */
    List<RssFetchData> getAfter(@Param("timestamp") Long timestamp,
                                @Param("articleDate") Long articleDate
    );

    /**
     * Extracts articles with records in translation section
     *
     * @param statusEnum by which status
     * @return list of the rss fetch data
     */
    List<RssFetchData> getNewsWithTranslationByStatus(@Param("statusName") ArticleTranslationStatusEnum statusEnum);

    /**
     * Extracts max untranslated article
     *
     * @return timestamp
     */
    Long getMaxTranslatedTimestamp();

    /**
     * Marks news as read
     *
     * @param rssDataCollection collection of news
     */
    void markAsReadNews(@Param("news") Collection<Long> rssDataCollection);

    /**
     * Performs inserting to the datasource list of translation data
     *
     * @param data list of data. Check for null before insert
     */
    void insertNewArticleTranslations(@Nonnull @Param("data") List<RssArticleTranslationEntity> data);

    /**
     * Performs updating article translation in appropriate DB
     *
     * @param response   ai translation response
     * @param statusEnum Which status of the article do you want to set
     * @param reason     some text description
     */
    void queryUpdateArticleTranslation(@Nonnull @Param("item") AiTranslationResponse response,
                                       @Nonnull @Param("status") ArticleTranslationStatusEnum statusEnum,
                                       @Param("reason") String reason);

    /**
     * Massive update status for group of items
     *
     * @param items                        list of items
     * @param articleTranslationStatusEnum status information
     */
    void queryUpdateStatusByListData(@Param("data") List<Long> items,
                                     @Param("status") ArticleTranslationStatusEnum articleTranslationStatusEnum);

    /**
     * Extracts articles by category and date range
     *
     * @param categoryId category ID
     * @param startTime  start timestamp
     * @param endTime    end timestamp
     * @return list of articles
     */
    List<RssFetchData> queryArticlesByCategoryAndDate(@Param("categoryId") int categoryId,
                                                        @Param("startTime") Long startTime,
                                                        @Param("endTime") Long endTime);

    /**
     * Extracts articles by category and date range with pagination
     *
     * @param categoryId category ID
     * @param startTime  start timestamp
     * @param endTime    end timestamp
     * @param offset     offset for pagination
     * @param limit      maximum number of articles to return
     * @return list of articles
     */
    List<RssFetchData> queryArticlesByCategoryAndDatePaginated(@Param("categoryId") int categoryId,
                                                                 @Param("startTime") Long startTime,
                                                                 @Param("endTime") Long endTime,
                                                                 @Param("offset") int offset,
                                                                 @Param("limit") int limit);

    /**
     * Counts articles by category and date range
     *
     * @param categoryId category ID
     * @param startTime  start timestamp
     * @param endTime    end timestamp
     * @return count of articles
     */
    int countArticlesByCategoryAndDate(@Param("categoryId") int categoryId,
                                        @Param("startTime") Long startTime,
                                        @Param("endTime") Long endTime);

    List<RssFetchData> queryArticlesByIds(@Param("ids") Collection<Long> ids);
}
