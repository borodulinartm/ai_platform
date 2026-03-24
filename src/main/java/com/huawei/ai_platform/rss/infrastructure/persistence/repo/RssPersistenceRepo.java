package com.huawei.ai_platform.rss.infrastructure.persistence.repo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.persistence.assembler.RssArticleTranslationMapper;
import com.huawei.ai_platform.rss.infrastructure.persistence.assembler.RssAssembler;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssCategoryDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssFeedDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssArticleTranslationEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFeedEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFetchData;
import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import com.huawei.ai_platform.rss.model.RssData;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import static com.huawei.ai_platform.common.Constant.ZONE;

/**
 * Repository layer for RSS subside
 *
 * @author Borodulin Artem
 * @since 2026.03.05
 */
@Repository
@RequiredArgsConstructor
public class RssPersistenceRepo {
    private final RssDao rssDao;
    private final RssCategoryDao rssCategoryDao;
    private final RssFeedDao rssFeedDao;
    private final RssArticleTranslationMapper rssArticleTranslationMapper;
    private final RssAssembler rssAssembler;

    /**
     * Extracts list of RSS data items
     *
     * @param dateToFind for which date do you want to find records?
     * @return list of RSS data
     */
    public List<RssData> getArticles(LocalDateTime dateToFind) {
        LocalDateTime startDay = dateToFind.with(LocalTime.MIN);
        LocalDateTime endDay = dateToFind.with(LocalTime.MAX);

        long milliStart = getAsMicro(startDay);
        long milliEnd = getAsMicro(endDay);

        List<RssFetchData> fetchedData = rssDao.queryArticlesBy(milliStart, milliEnd);
        return rssAssembler.convertFromFetchToRssData(fetchedData);
    }

    /**
     * Extracts date as micro value
     *
     * @param forDate for which date do you want extract data
     * @return microseconds
     */
    private long getAsMicro(LocalDateTime forDate) {
        Instant instant = forDate.atZone(ZONE).toInstant();
        long epochSecond = instant.getEpochSecond();
        int nanoAdjustment = instant.getNano();

        return epochSecond * 1_000_000L + nanoAdjustment / 1_000L;
    }

    /**
     * Marks as read collection of the data
     *
     * @param rssDataCollection Collection of the RSS data
     * @return Operation Result: success if operation has completed with OK, failed otherwise
     */
    public OperationResult markAsRead(@Nonnull Collection<Long> rssDataCollection) {
        rssDao.markAsReadNews(rssDataCollection);
        return OperationResult.builder().state(OperationResultEnum.SUCCESS).reason("Successfully read news").build();
    }

    /**
     * Extracts list of categories
     *
     * @return list of categories
     */
    public List<RssCategoryEntity> getCategories() {
        return rssCategoryDao.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * Extracts list of categories
     *
     * @return list of categories
     */
    public List<RssFeedEntity> getFeedEntity() {
        return rssFeedDao.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * Performs inserting data into datasource
     *
     * @param rssDataList list of data. Must be not null
     * @param statusEnum  status. Must be not null
     */
    @Transactional
    public void insertArticleTranslations(@Nonnull List<RssData> rssDataList, @Nonnull ArticleTranslationStatusEnum statusEnum) {
        List<RssArticleTranslationEntity> translatedToEntity = rssDataList.stream()
                .map(v -> rssArticleTranslationMapper.convert(v, statusEnum)).toList();
        rssDao.insertNewArticleTranslations(translatedToEntity);
    }

    @Transactional
    public void queryUpdateArticleTranslation(@Nonnull List<AiTranslationResponse> responses,
                                              @Nonnull ArticleTranslationStatusEnum statusEnum) {
        responses.forEach(v -> rssDao.queryUpdateArticleTranslation(v, statusEnum));
    }

    @Transactional
    public void queryUpdateStatusByListData(@Nonnull List<Long> idList, @Nonnull ArticleTranslationStatusEnum statusEnum) {
        rssDao.queryUpdateStatusByListData(idList, statusEnum);
    }
}
