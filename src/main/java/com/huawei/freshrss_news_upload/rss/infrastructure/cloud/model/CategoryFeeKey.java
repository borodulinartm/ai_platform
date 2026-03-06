package com.huawei.freshrss_news_upload.rss.infrastructure.cloud.model;

import com.huawei.freshrss_news_upload.rss.enums.RssTypeInfoEnum;
import lombok.*;

import java.util.Objects;

/**
 * Complex key for the RSS
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class CategoryFeeKey {
    private int categoryId;
    private int feedId;
    private RssTypeInfoEnum typeInfoEnum;

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, feedId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        CategoryFeeKey another = (CategoryFeeKey) obj;
        return this.categoryId == another.categoryId && this.feedId == another.feedId
                && this.typeInfoEnum == another.typeInfoEnum;
    }
}
