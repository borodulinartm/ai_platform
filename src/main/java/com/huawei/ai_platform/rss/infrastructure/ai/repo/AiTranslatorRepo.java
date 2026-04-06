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
        List<Long> listIds = List.of(request.getArticleId());

        int countAttempts = 1;
        while (countAttempts <= maxCountAttempts) {
            try {
                String resourceLocationZh = "prompt/translations/translation-prompt-zh.txt";
                String resourceLocationEn = "prompt/translations/translation-prompt-en.txt";
                String userPromptPath = "prompt/user-prompt.txt";

                String contentEn = vibeTranslating(request.getArticleContent(),
                        StringUtils.isNoneBlank(resourceLocationEn) ? resourceLocationEn : request.getArticleLink(), userPromptPath,
                        temperature
                );
                String contentZh = vibeTranslating(request.getArticleContent(),
                        StringUtils.isNoneBlank(resourceLocationEn) ? resourceLocationZh : request.getArticleLink(), userPromptPath,
                        temperature
                );

                String titleEn = vibeTranslating(request.getArticleTitle(), resourceLocationEn, userPromptPath, temperature);
                String titleZh = vibeTranslating(request.getArticleTitle(), resourceLocationZh, userPromptPath, temperature);

                return AiTranslationResponse.successResponse(request.getArticleId(), titleEn, titleZh, contentEn, contentZh);
            } catch (Exception exception) {
                log.warn("STAGE 3 vs 3: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}", countAttempts++, maxCountAttempts,
                        listIds.stream().map(Object::toString).collect(Collectors.joining(",")),
                        exception.getMessage());
            }
        }

        log.error("AI Translating: count attempts has exceeded; ID = {}", listIds.stream().map(Object::toString).collect(Collectors.joining(",")));

        return AiTranslationResponse.failureResponse(request.getArticleId(), "AI Translating: count attempts has exceeded");
    }

    /**
     * So many vibes
     *
     * @param data             data
     * @param systemPromptPath what a location
     * @param userPromptPath   where stores user prompt
     * @param passedTemp       passed temp
     * @return vibed text
     */
    private String vibeTranslating(String data, String systemPromptPath, String userPromptPath, Double passedTemp) {
        ClassPathResource systemPromptResource = new ClassPathResource(systemPromptPath);
        ClassPathResource userPromptResource = new ClassPathResource(userPromptPath);

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
}
