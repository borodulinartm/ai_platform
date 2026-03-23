package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.AiTranslationResponse;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

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

    public AiTranslatorRepo(ChatClient.Builder builder, ObjectMapper objectMapper) {
        chatClient = builder.defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Performs translation
     *
     * @param request list of the news that need to request
     * @return list of response data
     */
    public List<AiTranslationResponse> translate(@Nonnull List<AiTranslationRequest> request) {
        try {
            log.info("run translation. {}", Thread.currentThread().getName());

            File file = ResourceUtils.getFile("classpath:prompt/system-prompt.txt");

            Message systemMessage = new SystemMessage(Files.readString(file.toPath()));
            Message userMessage = new UserMessage(objectMapper.writeValueAsString(request));

            String res = chatClient.prompt(
                    new Prompt.Builder().messages(List.of(systemMessage, userMessage)).build()
            ).call().content();

            if (StringUtils.isBlank(res)) {
                throw new IllegalStateException("The result of the response is empty for some reason");
            }

            return objectMapper.readValue(res, new TypeReference<>() {});
        } catch (Exception exception) {
            log.error("An error has occurred during extracting data: {}", exception.getMessage());
            return Collections.emptyList();
        }
    }
}
