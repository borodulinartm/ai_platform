package com.huawei.ai_platform.rss.application.repo;

import com.huawei.ai_platform.rss.model.RssData;

import java.util.List;

/**
 * Interface which provides methods for translating
 *
 * @author Borodulin Artem
 * @since 2026.03.20
 */
public interface RssArticleTranslatorRepository {
    /**
     * Performs translating news
     *
     * @param compacts list of untranslated news
     * @return list of translated news
     */
    List<RssData> translate(List<RssData> compacts);

    /**
     * Extracts all untranslated news from datasource
     *
     * @return untranslated news
     */
    List<RssData> getNotTranslatedNews();
}
