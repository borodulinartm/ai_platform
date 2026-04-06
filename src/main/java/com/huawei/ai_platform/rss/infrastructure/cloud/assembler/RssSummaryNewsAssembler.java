package com.huawei.ai_platform.rss.infrastructure.cloud.assembler;

import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssArticleSummaryCloud;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssNewsSummaryCloud;
import com.huawei.ai_platform.rss.model.RssArticleSummary;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class RssSummaryNewsAssembler {
    /**
     * Converts from summary to cloud section
     *
     * @param input input list
     * @return list of summary cloud
     */
    public abstract List<RssNewsSummaryCloud> toSummaryCloud(List<RssNewsSummary> input);

    @Mapping(target = "articlesReport", source = "articles")
    public abstract RssNewsSummaryCloud toSummaryCloud(RssNewsSummary input);

    @Mapping(target = "articleTitleEn", source = "title")
    @Mapping(target = "articleAbstractEn", source = "articleAbstract")
    @Mapping(target = "articleTitleZh", source = "titleCn")
    @Mapping(target = "articleAbstractZh", source = "abstractCn")
    @Mapping(target = "backgroundEn", source = "background")
    @Mapping(target = "effectsEn", source = "effects")
    @Mapping(target = "eventSummaryEn", source = "eventSummary")
    @Mapping(target = "technologyAndInnovationEn", source = "technologyAndInnovation")
    @Mapping(target = "valueAndImpactEn", source = "valueAndImpact")
    @Mapping(target = "backgroundZh", source = "backgroundCn")
    @Mapping(target = "effectsZh", source = "effectsCn")
    @Mapping(target = "eventSummaryZh", source = "eventSummaryCn")
    @Mapping(target = "technologyAndInnovationZh", source = "technologyAndInnovationCn")
    @Mapping(target = "valueAndImpactZh", source = "valueAndImpactCn")
    public abstract RssArticleSummaryCloud toArticleSummaryCloud(RssArticleSummary input);
}
