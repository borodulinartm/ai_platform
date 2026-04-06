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
    FAILURE
}
