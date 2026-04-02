package com.huawei.ai_platform.rss.infrastructure.ai.scraper;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class WebScraperService {

    private final int timeoutMs;
    private final String userAgent;
    private final int maxContentLength;
    private final Map<String, Integer> failedDomains = new ConcurrentHashMap<>();
    private static final int MAX_FAILURES_PER_DOMAIN = 3;

    public WebScraperService(
        @Value("${ai.scraper.timeout:30000}") int timeoutMs,
        @Value("${ai.scraper.user-agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36}") String userAgent,
        @Value("${ai.scraper.max-content-length:50000}") int maxContentLength) {
        this.timeoutMs = timeoutMs;
        this.userAgent = userAgent;
        this.maxContentLength = maxContentLength;
    }

    public String scrapeContent(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }

        String domain = extractDomain(url);
        if (domain != null && isDomainBlacklisted(domain)) {
            log.debug("Skipping blacklisted domain: {}", domain);
            return "";
        }

        try {
            String content = CompletableFuture.supplyAsync(() -> fetchContent(url, domain))
                .get(timeoutMs, TimeUnit.MILLISECONDS);
            return truncate(content);
        } catch (TimeoutException e) {
            log.warn("Scraping timed out for URL: {} (domain: {})", url, domain);
            recordFailure(domain);
            return "";
        } catch (Exception e) {
            log.warn("Failed to scrape URL {}: {}", url, e.getMessage());
            recordFailure(domain);
            return "";
        }
    }

    public boolean isDomainBlacklisted(String domain) {
        if (domain == null) return false;
        Integer failures = failedDomains.get(domain);
        return failures != null && failures >= MAX_FAILURES_PER_DOMAIN;
    }

    public void resetFailures() {
        failedDomains.clear();
        log.info("Cleared all domain failure records");
    }

    public Map<String, Integer> getFailedDomains() {
        return Map.copyOf(failedDomains);
    }

    private void recordFailure(String domain) {
        if (domain == null) return;
        int newCount = failedDomains.merge(domain, 1, Integer::sum);
        if (newCount == MAX_FAILURES_PER_DOMAIN) {
            log.warn("Domain {} blacklisted after {} failures", domain, newCount);
        } else {
            log.debug("Domain {} failure count: {}", domain, newCount);
        }
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchContent(String url, String domain) {
        try {
            Document doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(timeoutMs)
                .followRedirects(true)
                .maxBodySize(maxContentLength * 2)
                .get();

            removeUnwantedElements(doc);

            String content = extractMainContent(doc);
            
            if (content.isBlank()) {
                content = doc.body() != null ? doc.body().text() : "";
            }

            if (content.isBlank()) {
                recordFailure(domain);
                return "";
            }

            return cleanContent(content);
        } catch (Exception e) {
            log.debug("Failed to fetch {}: {}", url, e.getMessage());
            recordFailure(domain);
            return "";
        }
    }

    private void removeUnwantedElements(Document doc) {
        doc.select("script, style, nav, header, footer, aside, .sidebar, .advertisement, .ad, .ads, .comments, .comment, .social-share, .related-posts, .recommended, noscript, iframe").remove();
        doc.select("[role=navigation], [role=banner], [role=complementary]").remove();
    }

    private String extractMainContent(Document doc) {
        String[] contentSelectors = {
            "article",
            "[role=main]",
            "main",
            ".post-content",
            ".article-content",
            ".entry-content",
            ".content",
            ".post-body",
            ".article-body",
            "#content",
            ".main-content"
        };

        for (String selector : contentSelectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                Element mainContent = elements.first();
                if (mainContent != null) {
                    String text = mainContent.text();
                    if (text.length() > 500) {
                        return text;
                    }
                }
            }
        }

        Elements paragraphs = doc.select("p");
        if (!paragraphs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() > 50) {
                    sb.append(text).append("\n\n");
                }
            }
            String content = sb.toString().trim();
            if (content.length() > 500) {
                return content;
            }
        }

        return "";
    }

    private String cleanContent(String content) {
        if (content == null) return "";
        
        return content
            .replaceAll("\\s+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    private String truncate(String content) {
        if (content == null) return "";
        if (content.length() <= maxContentLength) return content;
        return content.substring(0, maxContentLength) + "...";
    }
}
