package com.huawei.ai_platform.rss.infrastructure.ai.assembler;

import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.model.RssData;
import jakarta.annotation.Nonnull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for the AI translation data
 *
 * @author Borodulin Artem
 * @since 2026.03.23
 */
@Mapper(componentModel = "spring")
public abstract class AiTranslationMapper {
    /**
     * Performs mapping from the RSS data to the Ai translation request
     *
     * @param rssData rss data. Must be not null
     * @return Ai translation request
     */
    @Mapping(target = "articleTitle", source = "articleTitleEn")
    @Mapping(target = "id", source = "articleId")
    public abstract AiCleaningRequest convert(@Nonnull RssData rssData);
}
