package com.huawei.ai_platform.rss.application.service.impl.link;

import com.huawei.ai_platform.rss.application.service.RssLinkExtractor;
import com.huawei.ai_platform.rss.enums.LinkExtractionEnum;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The stupidest algorithm - just return an input
 *
 * @author Borodulin Artem
 * @since 2026.04.16
 */
@Component
@RequiredArgsConstructor
public class DefaultLinkExtractor implements RssLinkExtractor {
    @Override
    public String getUrl(@Nonnull String inputUrl) {
        return inputUrl;
    }

    @Nonnull
    @Override
    public LinkExtractionEnum getType() {
        return LinkExtractionEnum.DEFAULT;
    }
}
