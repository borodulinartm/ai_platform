package com.huawei.ai_platform.rss.infrastructure.ai.exceptions;


import lombok.Getter;
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

    @Getter
    private String input;

    @Getter
    private String output;

    public AiValidationException(List<String> message, String input, String output) {
        this.result = message;
        this.input = input;
        this.output = output;
    }

    public AiValidationException(String message, String input, String output) {
        this.result = List.of(message);
        this.input = input;
        this.output = output;
    }

    @Override
    public String getMessage() {
        if (CollectionUtils.isEmpty(result)) {
            return "";
        }

        return String.join(System.lineSeparator(), result);
    }
}
