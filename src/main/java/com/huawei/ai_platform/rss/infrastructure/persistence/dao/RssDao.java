package com.huawei.ai_platform.rss.infrastructure.persistence.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFetchData;
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
    List<RssFetchData> queryUnreadItemsByDate(
            @Param("startTime") long start,
            @Param("endTime") long end
    );

    /**
     * Marks news as read
     *
     * @param rssDataCollection collection of news
     */
    void markAsReadNews(@Param("news") Collection<Long> rssDataCollection);
}
