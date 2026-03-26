package com.huawei.ai_platform.rss.infrastructure.ai.assembler;

import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationRequest;
import com.huawei.ai_platform.rss.model.RssData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class AiTranslationMapper {
    @Mapping(target = "articleContentEn", source = "articleContent")
    public abstract AiTranslationRequest convert(RssData rssData);
}
