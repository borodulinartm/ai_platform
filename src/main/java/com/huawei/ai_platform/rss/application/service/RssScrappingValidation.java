package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping.AiScrappingRequest;

/**
 * Interface that checks RSS for neediness scrapping data from the URL
 *
 * @author Borodulin Artem
 * @since 2026.04.16
 */
public interface RssScrappingValidation {
    /**
     * Performs checking if we need scrap content of the article
     *
     * @param aiScrappingRequest content
     * @return true if scraping is required, false otherwise
     */
    boolean needScrap(AiScrappingRequest aiScrappingRequest);
}
