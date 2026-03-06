package com.huawei.freshrss_news_upload.rss.infrastructure.cloud.model;

import lombok.*;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class CategoryFeeKey {
    private int categoryId;
    private int feedId;

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
        return this.categoryId == another.categoryId && this.feedId == another.feedId;
    }
}
