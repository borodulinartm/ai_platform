package com.huawei.ai_platform.rss.infrastructure.cloud.assembler;

import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssArticleSummaryCloud;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssNewsSummaryCloud;
import com.huawei.ai_platform.rss.model.RssArticleSummary;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Assembler for the converting from the service layer to the cloud
 *
 * @author Borodulin Artem
 * @since 2026.03.12
 */
@Mapper(componentModel = "spring")
public abstract class RssSummaryNewsAssembler {
    /**
     * Converts from summary to cloud section
     *
     * @param input input list
     * @return list of summary cloud
     */
    public abstract List<RssNewsSummaryCloud> toSummaryCloud(List<RssNewsSummary> input);

    @Mapping(target = "articles", source = "articles")
    public abstract RssNewsSummaryCloud toSummaryCloud(RssNewsSummary input);

    @Mapping(target = "articleAbstract", source = "articleAbstract")
    public abstract RssArticleSummaryCloud toArticleSummaryCloud(RssArticleSummary input);
}
