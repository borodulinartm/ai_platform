package com.huawei.ai_platform.rss.infrastructure.persistence.repo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
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
import com.huawei.ai_platform.utils.DateUtils;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import static com.huawei.ai_platform.common.Constant.ZONE;
import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.FAILURE;
import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.INIT;
import static com.huawei.ai_platform.utils.DateUtils.getAsSeconds;

/**
 * Repository layer for RSS subside
 *
 * @author Borodulin Artem
 * @since 2026.03.05
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RssPersistenceRepo {
    @Value("${cloud.windowSize:1}")
    private long windowSize;

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

        long secondsStart = getAsSeconds(startDay, ZONE);
        long secondsEnd = getAsSeconds(endDay, ZONE);

        List<RssFetchData> fetchedData = rssDao.queryArticlesBy(secondsStart, secondsEnd);
        return rssAssembler.convertFromFetchToRssData(fetchedData);
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
     * Extracts from persistence repo not translated news
     *
     * @return List of the news that is not translated
     */
    public List<RssFetchData> getNotTranslatedNews() {
        Long latestRegisteredArticle = rssDao.getMaxTranslatedTimestamp();

        List<RssFetchData> fetchDataList = rssDao.getAfter(latestRegisteredArticle, null);

        fetchDataList.addAll(rssDao.getNewsWithTranslationByStatus(INIT, null));
        // For failed state try to retranslate news within window size
        // Don't worry, I perform automatic uploading to the cloud within that window size
        fetchDataList.addAll(rssDao.getNewsWithTranslationByStatus(
                FAILURE, DateUtils.getAsMicro(LocalDateTime.now().with(LocalTime.MIN).minusDays(windowSize), ZONE))
        );

        return fetchDataList;
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
        if (!CollectionUtils.isEmpty(translatedToEntity)) {
            rssDao.insertNewArticleTranslations(translatedToEntity);
        } else {
            log.info("insertArticleTranslations(): nothing to insert");
        }
    }

    /**
     * Transactional method for updating article translations
     *
     * @param responses  list of the responses. Must be not null
     * @param statusEnum status. Must be not null
     * @param reason     description (text errors)
     */
    @Transactional
    public void queryUpdateArticleTranslation(@Nonnull List<AiTranslationResponse> responses,
                                              @Nonnull ArticleTranslationStatusEnum statusEnum,
                                              String reason) {
        responses.forEach(v -> rssDao.queryUpdateArticleTranslation(v, statusEnum, reason));
    }

    /**
     * Updates status for batch of records. Transactional method
     *
     * @param idList     list of ID. Must be not null
     * @param statusEnum status. Also, must be not null
     */
    @Transactional
    public void queryUpdateStatusByListData(@Nonnull List<Long> idList, @Nonnull ArticleTranslationStatusEnum statusEnum) {
        if (!CollectionUtils.isEmpty(idList)) {
            rssDao.queryUpdateStatusByListData(idList, statusEnum);
        } else {
            log.info("queryUpdateStatusByListData(): Nothing to update");
        }
    }
}
