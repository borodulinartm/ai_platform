package com.huawei.ai_platform.rss.infrastructure.ai.driver;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI executor - driver class which implements some useful things
 *
 * @author Borodulin Artem
 * @since 2026.04.04
 */
@Component
@RequiredArgsConstructor
public class AiExecutor {
    private final ChatClient chatClient;

    public String performOperation(String systemPrompt, String userPrompt) {
        Message systemMessage = new SystemMessage(systemPrompt);
        Message userMessage = new UserMessage(userPrompt);

        String res = chatClient.prompt(
                new Prompt.Builder().messages(List.of(systemMessage, userMessage)).build()
        ).call().content();

        if (res == null) {
            return null;
        }

        return res.trim();
    }
}
