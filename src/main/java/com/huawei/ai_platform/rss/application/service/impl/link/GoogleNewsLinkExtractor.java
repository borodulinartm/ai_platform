package com.huawei.ai_platform.rss.application.service.impl.link;

import com.huawei.ai_platform.rss.application.service.RssLinkExtractor;
import com.huawei.ai_platform.rss.enums.LinkExtractionEnum;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * More complicated approach - extract from the Google News side
 *
 * @author Borodulin Artem
 * @since 2026.04.16
 */
@Component
@RequiredArgsConstructor
public class GoogleNewsLinkExtractor implements RssLinkExtractor {

    @Override
    public String getUrl(@Nonnull String inputUrl) {
        ProcessBuilder pb = new ProcessBuilder(
                "python3",
                "/Users/artem/Code/ai_platform/google_decoder_script/run.py",
                inputUrl
        );

        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                List<String> lineList = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lineList.add(line);
                }

                return lineList.getLast();

            } catch (IOException exception) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Nonnull
    @Override
    public LinkExtractionEnum getType() {
        return LinkExtractionEnum.GOOGLE;
    }
}
