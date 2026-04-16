package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.rss.enums.LinkExtractionEnum;
import jakarta.annotation.Nonnull;

/**
 * Link extractor strategy interface
 * Why we need? Because some providers don't provide direct URL to an article, they provide a proxy (e.g, Google News)
 * In ideal we only need extract just URL
 *
 * @author Borodulin Artem
 * @since 2026.04.16
 */
public interface RssLinkExtractor {
    /**
     * Extracting url by provided input data
     *
     * @param inputUrl input url
     * @return url
     */
    String getUrl(@Nonnull String inputUrl);

    /**
     * Extracts type of data
     *
     * @return enumeration
     */
    @Nonnull
    LinkExtractionEnum getType();
}
