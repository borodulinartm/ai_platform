package com.huawei.ai_platform.rss.infrastructure.ai.exceptions;


import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * AI invalid validation status exception
 *
 * @author Borodulin Artem
 * @since 2026.04.15
 */
public class AiValidationException extends RuntimeException {
    private List<String> result;

    public AiValidationException(List<String> message) {
        this.result = message;
    }

    public AiValidationException(String message) {
        this.result = List.of(message);
    }

    @Override
    public String getMessage() {
        if (CollectionUtils.isEmpty(result)) {
            return "";
        }

        return String.join(System.lineSeparator(), result);
    }
}
