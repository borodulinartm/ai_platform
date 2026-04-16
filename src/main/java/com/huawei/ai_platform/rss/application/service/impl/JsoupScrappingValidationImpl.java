package com.huawei.ai_platform.rss.application.service.impl;

import com.huawei.ai_platform.rss.application.service.RssScrappingValidation;
import com.huawei.ai_platform.rss.infrastructure.ai.model.scrapping.AiScrappingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Validator for scrapping using JSOUP checking
 *
 * @author Borodulin Artem
 * @since 2026.04.16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JsoupScrappingValidationImpl implements RssScrappingValidation {
    @Value("${ai.scrapping.symbolsThreshold}")
    private int threshold;

    @Override
    public boolean needScrap(AiScrappingRequest aiScrappingRequest) {
        Document document = Jsoup.parse(aiScrappingRequest.getArticleContent());

        String cleanedContent = document.select("script,hidden,style,form,img,a").remove().html().trim();
        if (cleanedContent.length() < threshold) {
            log.info("STAGE 2 vs 4: need scrapping for ID = {} due to low number of symbols ({})",
                    aiScrappingRequest.getId(), cleanedContent.length()
            );
            return true;
        }

        if (cleanedContent.endsWith("..")) {
            log.info("STAGE 2 vs 4: need scrapping for ID = {} due to multiple dot's. Look like article is not full",
                    aiScrappingRequest.getId()
            );

            return true;
        }

        return false;
    }
}
