package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationCompletedEvent;
import com.huawei.ai_platform.rss.infrastructure.ai.model.event.TranslationProcessingEvent;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.FAILURE;
import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.ArticleTranslationStatusEnum.FINISH;

/**
 * AI content generator
 *
 * @author Borodulin Artem
 * @since 2026.03.23
 */
@Component
@Slf4j
public class AiTranslatorRepo {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AiTranslatorRepo(ChatClient.Builder builder, ObjectMapper objectMapper,
                            ApplicationEventPublisher applicationEventPublisher) {
        chatClient = builder.defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Performs translation
     *
     * @param request article that need to request
     * @return response data
     */
    public AiTranslationResponse translate(@Nonnull AiTranslationRequest request) {
        List<Long> listIds = List.of(request.getArticleId());

        try {
            applicationEventPublisher.publishEvent(new TranslationProcessingEvent(listIds));

            log.info("Run translation for ID's = {}", listIds.stream().map(Object::toString).collect(Collectors.joining(",")));

            String resourceLocationZh = "classpath:prompt/system-prompt-for-content-zh.txt";
            String resourceLocationTitle = "classpath:prompt/system-prompt-for-title.txt";
            String resourceLocationEn = "classpath:prompt/system-prompt-for-content-en.txt";

            String contentZh = vibeTranslating(request.getArticleContentEn(), resourceLocationZh);
            String titleZh = vibeTranslating(request.getArticleTitleEn(), resourceLocationTitle);
            String cleanedEn = vibeTranslating(request.getArticleContentEn(), resourceLocationEn);

            AiTranslationResponse responseData = new AiTranslationResponse(request.getArticleId(), titleZh, contentZh, cleanedEn);
            applicationEventPublisher.publishEvent(new TranslationCompletedEvent(List.of(responseData), FINISH, "Success"));

            log.info("SUCCESSFULLY Finish translation for ID's = {}", listIds.stream().map(Object::toString).collect(Collectors.joining(",")));

            return responseData;
        } catch (Exception exception) {
            log.error("An error has occurred during extracting data: {}", exception.getMessage());

            List<AiTranslationResponse> mapToResponseWithEmptyData = listIds.stream()
                    .map(v -> new AiTranslationResponse(v, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY))
                    .toList();

            applicationEventPublisher.publishEvent(new TranslationCompletedEvent(mapToResponseWithEmptyData, FAILURE,
                    exception.getMessage())
            );

            return null;
        }
    }

    private String vibeTranslating(String data, String resourceLocation) throws IOException {
        File file = ResourceUtils.getFile(resourceLocation);

        Message systemMessage = new SystemMessage(Files.readString(file.toPath()));
        Message userMessage = new UserMessage(data);

        String res = chatClient.prompt(
                new Prompt.Builder().messages(List.of(systemMessage, userMessage)).build()
        ).call().content();

        if (StringUtils.isBlank(res)) {
            throw new IllegalStateException("The result of the response is empty for some reason");
        }

        return res;
    }
}
