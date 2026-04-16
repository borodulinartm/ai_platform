package com.huawei.ai_platform.rss.application.service.impl.link;

import com.huawei.ai_platform.rss.application.service.RssLinkExtractor;
import com.huawei.ai_platform.rss.enums.LinkExtractionEnum;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Link extractor factory
 *
 * @author Borodulin Artem
 * @since 2026.04.16
 */
@Component
@RequiredArgsConstructor
public class LinkExtractorFactory {
    private final List<RssLinkExtractor> rssLinkExtractors;
    private Map<LinkExtractionEnum, RssLinkExtractor> mapData;

    @PostConstruct
    private void afterExtractingData() {
        mapData = rssLinkExtractors.stream().collect(Collectors.toMap(RssLinkExtractor::getType, Function.identity(),
                (a, b) -> a)
        );
    }

    /**
     * Extracts strategy by provided link
     * @param link link
     * @return link extractor
     */
    @Nonnull
    public RssLinkExtractor getByLink(@Nonnull String link) {
        if (StringUtils.containsAny(link, "news.google")) {
            return mapData.get(LinkExtractionEnum.GOOGLE);
        }

        return mapData.get(LinkExtractionEnum.DEFAULT);
    }
}
