package com.huawei.ai_platform.rss.infrastructure.persistence.enums;

/**
 * Specific enum for the storing statuses of our tasks
 *
 * @author Borodulin Artem
 * @since 2026.03.24
 */
public enum ArticleTranslationStatusEnum {
    INIT,
    CLEANING_PROCESSING,
    TRANSLATING_PROCESSING,
    FINISH,
    FAILURE,
    SKIPPED;

    /**
     * Boolean flag means: whether article translated or not
     *
     * @return true if yes, false otherwise
     */
    public boolean isTranslated() {
        return this == FINISH || this == FAILURE || this == SKIPPED;
    }
}
