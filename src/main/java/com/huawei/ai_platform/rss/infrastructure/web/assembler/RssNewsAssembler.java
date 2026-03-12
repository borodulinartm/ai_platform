package com.huawei.ai_platform.rss.infrastructure.web.assembler;

import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import com.huawei.ai_platform.rss.infrastructure.web.model.RssNewsReportDto;
import com.huawei.ai_platform.rss.model.RssNewsSummary;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
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
     * @param input       list of an input data
     * @param mapCategory map: key -> category ID, value -> link to instance
     * @return List of rss summary data
     */
    public abstract List<RssNewsSummary> toAggregate(List<RssNewsReportDto> input,
                                                     @Context Map<Integer, RssCategoryEntity> mapCategory);

    /**
     * Applies category name for the RSS news input DTO
     *
     * @param input       RSS news article side
     * @param mapCategory map category by category ID
     */
    @AfterMapping
    public void applyCategoryName(@MappingTarget List<RssNewsSummary> input, @Context Map<Integer, RssCategoryEntity> mapCategory) {
        if (input != null && mapCategory != null) {
            input.forEach(article -> {
                RssCategoryEntity entity = mapCategory.get(article.getCategoryId());
                if (entity != null) {
                    article.setCategoryName(entity.getName());
                }
            });
        }
    }
}
