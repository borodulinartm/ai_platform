package com.huawei.ai_platform.rss.infrastructure.ai.driver;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * AI executor - driver class which implements some useful things
 *
 * @author Borodulin Artem
 * @since 2026.04.04
 */
@Component
@RequiredArgsConstructor
public class AiExecutor {
    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double defaultTemperature;

    private final ChatClient chatClient;

    /**
     * "Low-level" AI operation. This implementation uses spring AI (sync mode)
     *
     * @param systemPrompt system prompt
     * @param userPrompt   user prompt
     * @param temp         temperature
     * @return Response from AI
     */
    public String performOperation(String systemPrompt, String userPrompt, Double temp) {
        Double temperatureToPass = temp == null ? defaultTemperature : temp;

        Message systemMessage = new SystemMessage(systemPrompt);
        Message userMessage = new UserMessage(userPrompt);

        String res = chatClient.prompt(
                new Prompt.Builder().messages(List.of(systemMessage, userMessage))
                        .chatOptions(ChatOptions.builder().temperature(temperatureToPass).build())
                        .build()
        ).call().content();

        if (res == null) {
            return null;
        }

        if (res.toLowerCase(Locale.ENGLISH).contains("no_content")) {
            return StringUtils.EMPTY;
        }

        return res.trim();
    }
}
