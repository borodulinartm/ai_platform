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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    @Value("${classpath:prompt/system-prompt.txt}")
    private Resource systemPropmptResource;

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
            Message systemMessage = new SystemMessage(systemPropmptResource.getContentAsString(StandardCharsets.UTF_8));
            Message userMessage = new UserMessage(objectMapper.writeValueAsString(request));

            String res = chatClient.prompt(
                    new Prompt.Builder().messages(List.of(systemMessage, userMessage)).build()
            ).call().content();

            if (StringUtils.isBlank(res)) {
                throw new IllegalStateException("The result of the response is empty for some reason");
            }

            return objectMapper.readValue(res, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
