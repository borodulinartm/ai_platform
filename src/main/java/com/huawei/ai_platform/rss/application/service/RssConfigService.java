package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.rss.model.RssCategory;

import java.util.List;

/**
 * Interface for service config layer
 *
 * @author Borodulin Artem
 * @since 2026.03.12
 */
public interface RssConfigService {
    /**
     * Extracts list of categories
     *
     * @return List of categories
     */
    List<RssCategory> listCategories();
}
