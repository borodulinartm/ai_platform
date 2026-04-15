package com.huawei.ai_platform.rss.infrastructure.ai.exceptions;

/**
 * Exception for the null-result exception from AI
 *
 * @author Borodulin Artem
 * @since 2026.04.04
 */
public class AiNullResultException extends RuntimeException {
    public AiNullResultException(String message) {
        super(message);
    }

    public AiNullResultException() {
        super("");
    }
}
