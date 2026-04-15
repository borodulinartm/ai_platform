package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.driver.AiExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiInvalidStateException;
import com.huawei.ai_platform.rss.infrastructure.ai.exceptions.AiNullResultException;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningResponse;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssAttributeValue;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.RssAttributeTypeEnum.IMAGE_JPEG;
import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.RssAttributeTypeEnum.IMAGE_PNG;

/**
 * Cleaner section for the AI
 *
 * @author Borodulin Artem
 * @since 2026.04.04
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiCleaningArticlesRepo {
    private final AiExecutor aiExecutor;

    @Value("${ai.cleaning.countAttempts}")
    private int maxCountAttempts;

    @Value("${ai.cleaning.temperature}")
    private Double temperature;

    /**
     * Performs cleaning data for the input side
     *
     * @param cleaningRequest cleaning request
     * @return cleaning response
     */
    public @Nonnull AiCleaningResponse processCleaning(@Nonnull AiCleaningRequest cleaningRequest) {
        List<Long> listIds = List.of(cleaningRequest.getId());

        String systemPrompt = "prompt/cleaning/cleaning-prompt.txt";
        String userPrompt = "prompt/user-prompt.txt";

        int countAttempts = 1;
        while (countAttempts <= maxCountAttempts) {
            try {
                StringBuilder inputContent = new StringBuilder(cleaningRequest.getArticleContent());

                if (cleaningRequest.getAttributes() != null && !CollectionUtils.isEmpty(cleaningRequest.getAttributes().getEnclosures())) {
                    for (RssAttributeValue.Enclosure enclosure : cleaningRequest.getAttributes().getEnclosures()) {
                        if (enclosure.getType() != null && (enclosure.getType() == IMAGE_JPEG || enclosure.getType() == IMAGE_PNG)) {
                            inputContent.append(String.format("<img src=\"%s\"/>", enclosure.getUrl()));
                        }
                    }
                }

//                String cleanedTitle = exec(systemPrompt, userPrompt, cleaningRequest.getArticleTitle());
                String cleanedContent = exec(systemPrompt, userPrompt, inputContent.toString());

                return AiCleaningResponse.success(cleaningRequest.getId(), cleaningRequest.getArticleTitle(), cleanedContent, cleaningRequest.getArticleLink());
            } catch (Exception e) {
                log.warn("AI Cleaning side: Attempt {} vs {}: For ID = {} an error has occurred. Text = {}",
                        countAttempts++, maxCountAttempts,
                        listIds.stream().map(Object::toString).collect(Collectors.joining(",")),
                        e.getMessage()
                );
            }
        }

        log.error("AI Cleaning: count attempts has exceeded; ID = {}",
                listIds.stream().map(Object::toString).collect(Collectors.joining(",")));

        return AiCleaningResponse.failure(cleaningRequest.getId(), "AI Cleaning: count attempts has exceeded");
    }

    /**
     * Executes operation
     *
     * @param systemPromptPath path for the system prompt
     * @param userPromptPath   path for the user prompt
     * @param data             content which need to operate
     * @return result String output format from the AI
     */
    private String exec(String systemPromptPath, String userPromptPath, String data) {
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
        } catch (IOException e) {
            throw new AiInvalidStateException("An IO exception has occurred. Text = " + e.getMessage());
        }
    }
}
