package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiInvalidStateException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

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
    private final AiExecutor aiExecutor;

    @Value("${ai.cleaning.countAttempts}")
    private int maxCountAttempts;

    @Value("${ai.cleaning.temperature}")
    private Double temperature;


    /**
     * Performs translation
     *
     * @param request article that need to request
     * @return response data
     */
    public AiTranslationResponse translate(@Nonnull AiTranslationRequest request) {
        List<Long> listIds = List.of(request.getArticleId());

        int countAttempts = 1;

        while (true) {
            try {
                String resourceLocationZh = "prompt/translations/translation-prompt-zh.txt";
                String resourceLocationEn = "prompt/translations/translation-prompt-en.txt";
                String userPromptPath = "prompt/user-prompt.txt";

                String contentEn = vibeTranslating(request.getArticleContent(),
                        StringUtils.isNoneBlank(resourceLocationEn) ? resourceLocationEn : request.getArticleLink(), userPromptPath
                );
                String contentZh = vibeTranslating(request.getArticleContent(),
                        StringUtils.isNoneBlank(resourceLocationEn) ? resourceLocationZh : request.getArticleLink(), userPromptPath
                );

                String titleEn = vibeTranslating(request.getArticleTitle(), resourceLocationEn, userPromptPath);
                String titleZh = vibeTranslating(request.getArticleTitle(), resourceLocationZh, userPromptPath);

                return AiTranslationResponse.successResponse(request.getArticleId(), titleEn, titleZh, contentEn, contentZh);
            } catch (Exception exception) {
                if (countAttempts >= maxCountAttempts) {
                    log.error("STAGE 3 vs 3: An error has occurred during extracting data: {}; ID = {}", exception.getMessage(),
                            listIds.stream().map(Object::toString).collect(Collectors.joining(",")));

                    return AiTranslationResponse.failureResponse(request.getArticleId());
                } else {
                    log.warn("STAGE 3 vs 3: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}", countAttempts++, maxCountAttempts,
                            listIds.stream().map(Object::toString).collect(Collectors.joining(",")),
                            exception.getMessage());
                }
            }
        }
    }

    /**
     * So many vibes
     *
     * @param data             data
     * @param systemPromptPath what a location
     * @param userPromptPath   where stores user prompt
     * @return vibed text
     */
    private String vibeTranslating(String data, String systemPromptPath, String userPromptPath) {
        ClassPathResource systemPromptResource = new ClassPathResource(systemPromptPath);
        ClassPathResource userPromptResource = new ClassPathResource(userPromptPath);

        try (InputStream systemInputStream = systemPromptResource.getInputStream();
                InputStream userInputStream = userPromptResource.getInputStream()) {

            String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);
            String userPromptContent = String.format(new String(userInputStream.readAllBytes(), StandardCharsets.UTF_8), data);

            String result = aiExecutor.performOperation(systemPromptContent, userPromptContent, temperature);
            if (result == null) {
                throw new AiNullResultException("Result from the AI is null");
            }

            return result.trim();
        } catch (IOException exception) {
            throw new AiInvalidStateException("An IO exception has occurred. Text = " + exception.getMessage());
        }
    }
}
