package com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model;

import com.huawei.ai_platform.rss.infrastructure.cloud.model.CategoryFeeKey;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Typed key class structure for the pipeline context
 *
 * @author Borodulin Artem
 * @since 2026.04.26
 */
@AllArgsConstructor(staticName = "of")
@Getter
public class AiTypedKey<T> {
    private Class<T> type;
    private String name;

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(type).append(name).toHashCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        AiTypedKey<T> another = (AiTypedKey<T>) obj;
        return new EqualsBuilder().append(this.name, another.name)
                .append(this.type, another.type).isEquals();
    }
}
