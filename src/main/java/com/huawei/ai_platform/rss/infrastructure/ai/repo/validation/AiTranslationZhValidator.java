package com.huawei.ai_platform.rss.infrastructure.ai.repo.validation;

import com.huawei.ai_platform.rss.infrastructure.ai.model.validation.ValidationResponse;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * ZH validator for the Ai translation
 *
 * @author Borodulin Artem
 * @since 2026.04.15
 */
@Component("aiTranslationValidatorZh")
@RequiredArgsConstructor
public class AiTranslationZhValidator implements IAiValidation<String> {

    private static final Pattern CHINESE = Pattern.compile("[\\u4E00-\\u9FFF]");
    private static final Pattern LATIN = Pattern.compile("[a-zA-Z]");
    private static final Pattern RELEVANT = Pattern.compile("[\\p{L}\\p{N}]");

    @Nonnull
    @Override
    public ValidationResponse validate(@Nonnull String request) {
        if (StringUtils.isBlank(request)) {
            return ValidationResponse.failure("The response cannot be null");
        }

        long total = 0L;
        long chinese = 0L;
        long english = 0L;

        for (char item : request.toCharArray()) {
            String stringItem = String.valueOf(item);
            if (RELEVANT.matcher(stringItem).matches()) {
                ++total;

                if (CHINESE.matcher(stringItem).matches()) {
                    ++chinese;
                } else if (LATIN.matcher(stringItem).matches()) {
                    ++english;
                }
            }
        }

        return getValidationResult(total, chinese, english);
    }

    /**
     *
     * @param total   count total symbols
     * @param chinese count chinese symbols
     * @param english count english symbols
     * @return ValidationResponse status
     */
    private ValidationResponse getValidationResult(long total, long chinese, long english) {
        if (total == 0) {
            return ValidationResponse.failure("Total is 0");
        }

        if (chinese == 0) {
            return ValidationResponse.failure("Count of Chinese symbols is 0");
        }

        // Maybe I'll add more checks in the future :)

        return ValidationResponse.success();
    }
}
