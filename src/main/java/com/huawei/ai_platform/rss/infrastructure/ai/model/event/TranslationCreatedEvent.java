package com.huawei.ai_platform.rss.infrastructure.ai.model.event;

import com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum;
import com.huawei.ai_platform.rss.model.RssData;
import lombok.*;

import java.util.List;

/**
 * Wrapper class for storing translation starting event
 *
 * @author Borodulin Artem
 * @since 2026.03.24
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TranslationCreatedEvent {
    private List<RssData> records;
    private ArticleTranslationStatusEnum statusEnum;
}
