package com.huawei.ai_platform.rss.infrastructure.ai.repo;

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
    private static final int COUNT_ATTEMPTS = 10;

    private final ChatClient chatClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AiTranslatorRepo(ChatClient.Builder builder,
                            ApplicationEventPublisher applicationEventPublisher) {
        chatClient = builder.defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
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

        int countAttempts = 1;

        while (true) {
            try {
                applicationEventPublisher.publishEvent(new TranslationProcessingEvent(listIds));

                log.info("Run translation for ID's = {}", listIds.stream().map(Object::toString).collect(Collectors.joining(",")));

                String resourceLocationZh = "classpath:prompt/system-prompt-for-content-zh.txt";
                String resourceLocationTitle = "classpath:prompt/system-prompt-for-title.txt";
                String resourceLocationEn = "classpath:prompt/system-prompt-for-content-en.txt";
                String userPromptPath = "classpath:prompt/user-prompt.txt";

                String contentZh = vibeTranslating(request.getArticleContentEn(), resourceLocationZh, userPromptPath);
                String titleZh = vibeTranslating(request.getArticleTitleEn(), resourceLocationTitle, userPromptPath);
                String cleanedEn = vibeTranslating(request.getArticleContentEn(), resourceLocationEn, userPromptPath);

                AiTranslationResponse responseData = AiTranslationResponse.successResponse(request.getArticleId(),
                        titleZh, cleanedEn, contentZh
                );
                applicationEventPublisher.publishEvent(new TranslationCompletedEvent(List.of(responseData), FINISH, "Success"));

                log.info("SUCCESSFULLY Finish translation for ID's = {}", listIds.stream().map(Object::toString)
                        .collect(Collectors.joining(","))
                );

                return responseData;
            } catch (Exception exception) {
                if (countAttempts >= COUNT_ATTEMPTS) {
                    log.error("An error has occurred during extracting data: {}; ID = {}", exception.getMessage(),
                            listIds.stream().map(Object::toString).collect(Collectors.joining(",")));

                    AiTranslationResponse aiTranslationResponse = AiTranslationResponse.failureResponse(request.getArticleId());
                    applicationEventPublisher.publishEvent(new TranslationCompletedEvent(List.of(aiTranslationResponse), FAILURE,
                            exception.getMessage())
                    );

                    return aiTranslationResponse;
                } else {
                    log.warn("Attempt {} vs {}: For ID = {} an error has occurred. Text = {}", countAttempts++, COUNT_ATTEMPTS,
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
     * @throws IOException if exception has occurred
     */
    private String vibeTranslating(String data, String systemPromptPath, String userPromptPath) throws IOException {
        File systemPromptFile = ResourceUtils.getFile(systemPromptPath);
        File userPromptFile = ResourceUtils.getFile(userPromptPath);

        Message systemMessage = new SystemMessage(Files.readString(systemPromptFile.toPath()));
        Message userMessage = new UserMessage(String.format(
                Files.readString(userPromptFile.toPath()), data
        ));

        String res = chatClient.prompt(
                new Prompt.Builder().messages(List.of(systemMessage, userMessage)).build()
        ).call().content();

        if (StringUtils.isBlank(res)) {
            throw new IllegalStateException("The result of the response is empty for some reason");
        }

        return res;
    }
}
