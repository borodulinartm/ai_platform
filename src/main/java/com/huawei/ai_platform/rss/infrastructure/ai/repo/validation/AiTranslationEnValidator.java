package com.huawei.ai_platform.rss.infrastructure.ai.repo.validation;

import com.huawei.ai_platform.rss.infrastructure.ai.model.validation.ValidationResponse;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator EN for the AI translation
 *
 * @author Borodulin Artem
 * @since 2026.04.15
 */
@Component("aiTranslationValidatorEn")
@RequiredArgsConstructor
public class AiTranslationEnValidator implements IAiValidation<String> {

    private static final Pattern CYRILLIC = Pattern.compile("[а-яА-ЯёЁ]");
    private static final Pattern CHINESE = Pattern.compile("[\\u4E00-\\u9FFF]");

    @Override
    public @Nonnull ValidationResponse validate(@Nonnull String checkingContent) {
        if (StringUtils.isBlank(checkingContent)) {
            return ValidationResponse.failure("Provided content is blank");
        }

        String cleaned = sanitizeInput(checkingContent);
        long countCyrillic = getCountMatches(cleaned, CYRILLIC);
        if (countCyrillic > 0) {
            return ValidationResponse.failure("Find Cyrillic symbols. Not valid. Need retranslation");
        }

        long size = cleaned.length();
        long countZhSymbols = getCountMatches(cleaned, CHINESE);

        double ratio = (double) countZhSymbols / size;
        ValidationResponse failureDueToOverloadingChinese = ValidationResponse.failure("The ratio of Chinese symbols is too high");

        if (size < 100) {
            if (ratio > 0.25) {
                return failureDueToOverloadingChinese;
            }
        } else if (size < 500) {
            if (ratio > 0.1) {
                return failureDueToOverloadingChinese;
            }
        } else {
            if (ratio > 0.01) {
                return failureDueToOverloadingChinese;
            }
        }

        return ValidationResponse.success();
    }

    /**
     * Performs count matching data
     *
     * @param inputString input string data
     * @param pattern     pattern
     * @return count matches by pattern
     */
    private long getCountMatches(String inputString, Pattern pattern) {
        long countResults = 0L;
        Matcher matcher = pattern.matcher(inputString);

        while (matcher.find()) {
            ++countResults;
        }

        return countResults;
    }

    /**
     * Performs cleaning data from the input
     *
     * @param input input (may contain images)
     * @return sanitaized strings
     */
    private String sanitizeInput(String input) {
        Document document = Jsoup.parse(input);
        return document.select("img").remove().html();
    }
}
