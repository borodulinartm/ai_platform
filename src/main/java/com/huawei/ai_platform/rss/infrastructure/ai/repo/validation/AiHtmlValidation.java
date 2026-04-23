package com.huawei.ai_platform.rss.infrastructure.ai.repo.validation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Validation for the checking for existing HTML side
 *
 * @author Borodulin Artem
 * @since 2026.04.21
 */
public class AiHtmlValidation implements BiPredicate<String, String> {

    @Override
    public boolean test(String before, String after) {
        Document document = Jsoup.parse(after);

        Elements elements = document.getAllElements();

        Set<String> uniqueTags = new HashSet<>();
        for (Element item : elements) {
            uniqueTags.add(item.tagName().toLowerCase(Locale.ENGLISH));
        }

        // If we have more than 1 tag - return false, validation does not successful
        if (uniqueTags.size() > 1) {
            return false;
        } else if (uniqueTags.size() == 1) {
            return uniqueTags.contains("img");
        }

        return true;
    }
}
