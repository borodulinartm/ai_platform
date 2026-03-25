package com.huawei.ai_platform.rss.infrastructure.persistence.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationResponse;
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
                                @Param("articleDate") Long articleDate);

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

    void queryUpdateArticleTranslation(@Nonnull @Param("item") AiTranslationResponse response,
                                       @Nonnull @Param("status") ArticleTranslationStatusEnum statusEnum,
                                       @Param("reason") String reason);

    void queryUpdateStatusByListData(@Param("data") List<Long> items,
                                     @Param("status") ArticleTranslationStatusEnum articleTranslationStatusEnum);
}
