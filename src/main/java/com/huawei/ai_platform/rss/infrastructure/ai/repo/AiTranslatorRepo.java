package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiInvalidStateException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiValidationException;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.model.validation.ValidationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.repo.validation.IAiValidation;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.huawei.ai_platform.common.OperationResultEnum.FAILURE;
import static com.huawei.ai_platform.common.OperationResultEnum.SUCCESS;

/**
 * AI content generator
 *
 * @author Borodulin Artem
 * @since 2026.03.23
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiTranslatorRepo {
    public static final String RESOURCE_LOCATION_ZH = "prompt/translations/translation-prompt-zh.txt";
    public static final String RESOURCE_LOCATION_EN = "prompt/translations/translation-prompt-en.txt";
    public static final String USER_PROMPT_PATH = "prompt/user-prompt.txt";

    private final AiExecutor aiExecutor;
    private final IAiValidation<String> aiTranslationValidatorZh;
    private final IAiValidation<String> aiTranslationValidatorEn;

    @Value("${ai.translating.countAttempts}")
    private int maxCountAttempts;

    @Value("${ai.translating.temperature}")
    private Double temperature;

    /**
     * Performs translation
     *
     * @param request article that need to request
     * @return response data
     */
    public AiTranslationResponse translate(@Nonnull AiTranslationRequest request) {
        TranslationResponseWrapper titleEn = runTranslation(request.getArticleId(), request.getArticleTitle(),
                RESOURCE_LOCATION_EN, aiTranslationValidatorEn);
        TranslationResponseWrapper titleZh = runTranslation(request.getArticleId(), request.getArticleTitle(),
                RESOURCE_LOCATION_ZH, aiTranslationValidatorZh);
        TranslationResponseWrapper contentEn = runTranslation(request.getArticleId(), request.getArticleContent(),
                RESOURCE_LOCATION_EN, aiTranslationValidatorEn);
        TranslationResponseWrapper contentZh = runTranslation(request.getArticleId(), request.getArticleContent(),
                RESOURCE_LOCATION_ZH, aiTranslationValidatorZh);

        if (titleEn.isFailed() || titleZh.isFailed() || contentEn.isFailed() || contentZh.isFailed()) {
            return AiTranslationResponse.failureResponse(request.getArticleId(), "AI Translating: count attempts has exceeded");
        }

        return AiTranslationResponse.successResponse(request.getArticleId(), titleEn.getTranslatedText(),
                titleZh.getTranslatedText(), contentEn.getTranslatedText(), contentZh.getTranslatedText());
    }

    /**
     * Runs translation
     *
     * @param id               for which article id do you want translate
     * @param content          content which needs translation
     * @param systemPromptPath path for the prompt
     * @param validator        validator to check the results
     * @return local wrapper of the state, message (if failed) and content translation
     */
    private TranslationResponseWrapper runTranslation(Long id, String content, String systemPromptPath, IAiValidation<String> validator) {
        int countAttempts = 1;

        // I give 10 attempts for translating. If we exceed that limit go away
        while (countAttempts <= maxCountAttempts) {
            try {
                String contentTranslated = vibeTranslating(content, systemPromptPath, temperature);

                ValidationResponse response = runValidation(contentTranslated, validator);
                if (response.isFailed()) {
                    throw new AiValidationException(response.getErrors());
                }

                return TranslationResponseWrapper.of(SUCCESS, contentTranslated, "");
            } catch (Exception exception) {
                log.warn("STAGE 3 vs 3: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}", countAttempts++, maxCountAttempts,
                        id, exception.getMessage());
            }
        }

        log.error("AI Translating: count attempts has exceeded; ID = {}", id);
        return TranslationResponseWrapper.of(FAILURE, content, "AI Translating: count attempts has exceeded");
    }

    /**
     * So many vibes
     *
     * @param data             data
     * @param systemPromptPath what a location
     * @param passedTemp       passed temp
     * @return vibed text
     */
    private String vibeTranslating(String data, String systemPromptPath, Double passedTemp) {
        ClassPathResource systemPromptResource = new ClassPathResource(systemPromptPath);
        ClassPathResource userPromptResource = new ClassPathResource(AiTranslatorRepo.USER_PROMPT_PATH);

        try (InputStream systemInputStream = systemPromptResource.getInputStream();
             InputStream userInputStream = userPromptResource.getInputStream()) {

            String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);
            String userPromptContent = String.format(new String(userInputStream.readAllBytes(), StandardCharsets.UTF_8), data);

            String result = aiExecutor.performOperation(systemPromptContent, userPromptContent, passedTemp);
            if (result == null) {
                throw new AiNullResultException("Result from the AI is null");
            }

            return result.trim();
        } catch (IOException exception) {
            throw new AiInvalidStateException("An IO exception has occurred. Text = " + exception.getMessage());
        }
    }

    /**
     * Checks output from the AI. The output must be in the same vibe level. Otherwise, error
     *
     * @param input         input (translated text from the AI)
     * @param iAiValidation validation strategy. Each strategy defines own vibe
     * @return Validation response
     */
    private ValidationResponse runValidation(String input, IAiValidation<String> iAiValidation) {
        if (input == null) {
            return ValidationResponse.failure("Input is null. Run again");
        }

        // Maybe content is empty. In that case, nothing to translate it's ok
        if (StringUtils.isBlank(input)) {
            return ValidationResponse.success();
        }

        return iAiValidation.validate(input);
    }

    /**
     * Inner helpful class for storing translation response. Works only in that class
     */
    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    private static class TranslationResponseWrapper {
        private OperationResultEnum status;
        private String translatedText;

        private String errorMessage;

        public boolean isFailed() {
            return status == FAILURE;
        }

    }
}
