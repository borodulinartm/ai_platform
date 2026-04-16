package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiInvalidStateException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping.AiScrappingRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping.AiScrappingResponse;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * AI-side for executing scrapping operations
 *
 * @author Borodulin Artem
 * @since 2026.04.16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiScrappingRepo {
    private final AiExecutor aiExecutor;

    @Value("${ai.scrapping.countAttempts}")
    private int maxCountAttempts;

    @Value("${ai.scrapping.temperature}")
    private Double temperature;

    public @Nonnull AiScrappingResponse runScrapping(@Nonnull AiScrappingRequest request) {
        String systemPrompt = "prompt/scrapping/scrapping-content.txt";
        int countAttempts = 1;

        while (countAttempts <= maxCountAttempts) {
            try {
                String scrappedContent = exec(systemPrompt, request.getArticleLink());
                return AiScrappingResponse.success(request.getId(), request.getArticleTitle(), scrappedContent,
                        request.getArticleLink(), request.getAttributes()
                );
            } catch (Exception e) {
                log.warn("AI Scrapping side: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}",
                        countAttempts++, maxCountAttempts, request.getId(), e.getMessage()
                );
            }
        }

        return AiScrappingResponse.failure(request.getId(), "Exceeded max attempts for ID = " + request.getId());
    }

    /**
     * Executes operation
     *
     * @param systemPromptPath path for the system prompt
     * @param data             content which need to operate
     * @return result String output format from the AI
     */
    private String exec(String systemPromptPath, String data) {
        ClassPathResource systemPromptResource = new ClassPathResource(systemPromptPath);

        try (InputStream systemInputStream = systemPromptResource.getInputStream()) {
            String systemPromptContent = new String(systemInputStream.readAllBytes(), StandardCharsets.UTF_8);

            String result = aiExecutor.performOperation(systemPromptContent, data, temperature);
            if (result == null) {
                throw new AiNullResultException("Result from the AI is null");
            }

            return result.trim();
        } catch (IOException e) {
            throw new AiInvalidStateException("An IO exception has occurred. Text = " + e.getMessage());
        }
    }
}
