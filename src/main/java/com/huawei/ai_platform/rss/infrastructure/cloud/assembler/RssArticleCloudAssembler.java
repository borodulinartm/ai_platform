package com.huawei.ai_platform.rss.infrastructure.cloud.assembler;

import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssArticleCloud;
import com.huawei.ai_platform.rss.model.RssData;
import org.jsoup.parser.Parser;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.util.CollectionUtils;

import java.util.List;

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
        if (target != null) {
            if (data != null) {
                target.setCategoryId(data.getRssCategory().getCategoryId());
                target.setFeedId(data.getFeed().getFeedId());
            }

            if (!CollectionUtils.isEmpty(target.getArticleTags())) {
                List<String> filteredTrash = target.getArticleTags().stream()
                        .map(v -> Parser.unescapeEntities(v, true)).toList();
                target.setArticleTags(filteredTrash);
            }
        }
    }
}
