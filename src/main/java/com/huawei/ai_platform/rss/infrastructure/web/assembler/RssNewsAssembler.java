package com.huawei.ai_platform.rss.infrastructure.web.assembler;

import com.huawei.ai_platform.rss.infrastructure.web.model.RssReportDto;
import com.huawei.ai_platform.rss.model.RssCategory;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

import java.util.Map;

/**
 * Assembler (converter) for the RSS news summary
 *
 * @author Borodulin Artem
 * @since 2026.03.12
 */
@Mapper(componentModel = "spring")
public abstract class RssNewsAssembler {
    /**
     * Central converter method for the DTO to Aggregate layer
     *
     * @param input       input data
     * @param mapCategory map: key -> category ID, value -> link to instance
     * @return rss summary data
     */
    public abstract RssNewsSummary toAggregate(RssReportDto input,
                                               @Context Map<Integer, RssCategory> mapCategory);
}
