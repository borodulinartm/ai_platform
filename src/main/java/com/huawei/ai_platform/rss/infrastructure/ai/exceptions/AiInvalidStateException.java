package com.huawei.ai_platform.rss.infrastructure.ai.exceptions;

/**
 * Exception for some invalid state
 *
 * @author Borodulin Artem
 * @since 2026.04.04
 */
public class AiInvalidStateException extends RuntimeException {
    public AiInvalidStateException(String message) {
        super(message);
    }

    public AiInvalidStateException(Throwable throwable) {
        super(throwable);
    }

    public AiInvalidStateException() {
        super();
    }
}
