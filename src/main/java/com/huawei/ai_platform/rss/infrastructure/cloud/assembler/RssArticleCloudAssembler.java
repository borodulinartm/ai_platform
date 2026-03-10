package com.huawei.ai_platform.rss.infrastructure.cloud.assembler;

import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssArticleCloud;
import com.huawei.ai_platform.rss.model.RssData;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * Mapper in the cloud section
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Mapper(componentModel = "spring")
public abstract class RssArticleCloudAssembler {
    /**
     * Converter from Aggregate to cloud structure
     *
     * @param input input data
     * @return of articles
     */
    public abstract RssArticleCloud convertToCloudStructure(RssData input);

    /**
     * Process after mapping data
     *
     * @param data   input aggregate
     * @param target target mapping
     */
    @AfterMapping
    public void afterMapping(RssData data, @MappingTarget RssArticleCloud target) {
        if (target != null && data != null) {
            target.setCategoryId(data.getRssCategory().getCategoryId());
            target.setFeedId(data.getFeed().getFeedId());
        }
    }
}
