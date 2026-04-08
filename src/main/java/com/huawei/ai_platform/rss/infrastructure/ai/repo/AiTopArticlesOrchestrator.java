package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.rss.infrastructure.ai.dto.ArticleRanking;
import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssNewsSummaryCloud;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssCategoryDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFetchData;
import com.huawei.ai_platform.rss.infrastructure.ai.scraper.WebScraperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.io.InputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AiTopArticlesOrchestrator {

    private static final int MAX_ATTEMPTS = 5;
    private static final int TOP_ARTICLES_COUNT = 10;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final RssCategoryDao rssCategoryDao;
    private final RssDao rssDao;
    private final WebScraperService webScraperService;

    private final int batchSize;
    private final int threadPoolSize;
    private final int timeoutMs;
    private final Path llmLogDir;
    private final String reasoningModel;
    private final String summaryModel;
    private final Double rankingTemperature;
    private final Double summaryTemperature;
    
    public AiTopArticlesOrchestrator(
        ChatClient chatClient,
        ObjectMapper objectMapper,
        RssCategoryDao rssCategoryDao,
        RssDao rssDao,
        WebScraperService webScraperService,
        @Value("${ai.processing.batch-size:100}") int batchSize,
        @Value("${ai.processing.thread-pool-size:5}") int threadPoolSize,
        @Value("${ai.processing.timeout:600000}") int timeoutMs,
        @Value("${ai.processing.log-dir:./logs/llm}") String logDir,
        @Value("${ai.processing.reasoning-model:deepseek/deepseek-v3.2}") String reasoningModel,
        @Value("${ai.processing.ranking-temperature:0.1}") Double rankingTemperature,
        @Value("${ai.processing.summary-model:deepseek/deepseek-v3.2}") String summaryModel,
        @Value("${ai.processing.summary-temperature:0.4}") Double summaryTemperature) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.rssCategoryDao = rssCategoryDao;
        this.rssDao = rssDao;
        this.webScraperService = webScraperService;
        this.batchSize = batchSize;
        this.threadPoolSize = threadPoolSize;
        this.timeoutMs = timeoutMs;
        this.llmLogDir = Paths.get(logDir);
        this.reasoningModel = reasoningModel;
        this.rankingTemperature = rankingTemperature;
        this.summaryModel = summaryModel;
        this.summaryTemperature = summaryTemperature;
        try {
            Files.createDirectories(llmLogDir);
        } catch (IOException e) {
            log.warn("Failed to create LLM log directory: {}", e.getMessage());
        }
    }

    public List<RssNewsSummaryCloud> processArticles(long startTime, long endTime) {
        String runTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        List<RssNewsSummaryCloud> allSummaries = new ArrayList<>();
        
        webScraperService.resetFailures();

        List<RssCategoryEntity> categoryEntities = rssCategoryDao.selectList(null);
        log.info("Found {} categories to process", categoryEntities.size());

        List<CategoryArticles> categoriesWithArticles = new ArrayList<>();
        for (var category : categoryEntities) {
            try {
                int totalCount = rssDao.countArticlesByCategoryAndDate(category.getId(), startTime, endTime);
                log.info("Category {} has {} articles", category.getName(), totalCount);
                
                if (totalCount > 0) {
                    List<RssFetchData> fetchData = rssDao.queryArticlesByCategoryAndDatePaginated(
                        category.getId(), startTime, endTime, 0, totalCount);
                    List<ArticleData> articles = toArticleDataList(fetchData);
                    log.info("Fetched {} articles for category {}", articles.size(), category.getName());
                    categoriesWithArticles.add(new CategoryArticles(category.getId(), category.getName(), articles, runTimestamp));
                }
            } catch (Exception e) {
                log.error("Failed to fetch articles for category {}: {}", category.getName(), e.getMessage());
            }
        }

        List<BatchContext> allBatches = new ArrayList<>();
        for (var ca : categoriesWithArticles) {
            var batches = splitIntoBatches(ca.articles(), batchSize);
            log.info("Category {} split into {} batches", ca.categoryName(), batches.size());
            for (int i = 0; i < batches.size(); i++) {
                allBatches.add(new BatchContext(
                    ca.categoryId(), ca.categoryName(), batches.get(i), i + 1, batches.size(), ca.runTimestamp()
                ));
            }
        }
        
        log.info("Total {} batches to process across all categories", allBatches.size());

        Map<Integer, List<ArticleScore>> scoresByCategory = new ConcurrentHashMap<>();
        for (var ca : categoriesWithArticles) {
            scoresByCategory.put(ca.categoryId(), Collections.synchronizedList(new ArrayList<>()));
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize)) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (var batchCtx : allBatches) {
                futures.add(CompletableFuture.runAsync(() -> {
                    log.info(
                        "Ranking batch {}/{} for category {}",
                        batchCtx.batchNum(), batchCtx.totalBatches(), batchCtx.categoryName()
                    );
                    var scores = rankBatch(
                        batchCtx.categoryId(), 
                        batchCtx.categoryName(), 
                        batchCtx.batch(), 
                        batchCtx.batchNum(), 
                        batchCtx.totalBatches(),
                        batchCtx.runTimestamp()
                    );
                    scoresByCategory.get(batchCtx.categoryId()).addAll(scores);
                    log.info(
                        "Completed batch {}/{} for category {} - got {} scores",
                        batchCtx.batchNum(), batchCtx.totalBatches(), batchCtx.categoryName(), scores.size()
                    );
                }, executor));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            for (var ca : categoriesWithArticles) {
                log.info(
                    "All batches processed for category {} - total {} scores",
                    ca.categoryName(), scoresByCategory.get(ca.categoryId()).size()
                );
            }
        }

        List<CompletableFuture<RssNewsSummaryCloud>> summaryFutures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize)) {
            for (var ca : categoriesWithArticles) {
                var scores = scoresByCategory.get(ca.categoryId());
                if (scores.isEmpty()) {
                    log.warn("No scores for category {}", ca.categoryName());
                    continue;
                }
                
                List<Long> topIds = scores.stream()
                    .sorted(Comparator.comparingDouble(ArticleScore::score).reversed())
                    .limit(TOP_ARTICLES_COUNT)
                    .map(ArticleScore::id)
                    .toList();
                
                if (topIds.isEmpty()) {
                    log.warn("No top articles for category {}", ca.categoryName());
                    continue;
                }

                summaryFutures.add(CompletableFuture.supplyAsync(() -> {
                    log.info("Generating summary for category {} with {} top articles", ca.categoryName(), topIds.size());
                    Map<Long, Double> scoreMap = scores.stream()
                        .collect(Collectors.toMap(ArticleScore::id, ArticleScore::score));
                    List<RssFetchData> fetchData = rssDao.queryArticlesByIds(topIds);
                    var topArticles = toArticleDataListWithScores(fetchData, scoreMap);
                    var topArticlesWithContent = scrapeArticleContent(topArticles);
                    RssNewsSummaryCloud summary = generateSummaries(ca.categoryId(), ca.categoryName(), topArticlesWithContent, ca.runTimestamp());
                    log.info("Completed summary for category {}", ca.categoryName());
                    return summary;
                }, executor));
            }

            for (var future : summaryFutures) {
                try {
                    RssNewsSummaryCloud summary = future.get();
                    if (summary != null) {
                        allSummaries.add(summary);
                    }
                } catch (Exception e) {
                    log.error("Summary generation failed: {}", e.getMessage());
                }
            }
        }

        var blacklisted = webScraperService.getFailedDomains().entrySet().stream()
            .filter(e -> e.getValue() >= 3)
            .map(Map.Entry::getKey)
            .toList();
        if (!blacklisted.isEmpty()) {
            log.info("Blacklisted domains (3+ failures): {}", blacklisted);
        }
        
        log.info("Successfully processed {} categories", allSummaries.size());
        return allSummaries;
    }

    private List<ArticleData> scrapeArticleContent(List<ArticleData> articles) {
        List<ArticleData> result = new ArrayList<>();
        
        for (var article : articles) {
            try {
                String scrapedContent = "";
                
                if (article.link() != null && !article.link().isBlank()) {
                    log.debug("Scraping content for article {}: {}", article.id(), article.link());
                    scrapedContent = webScraperService.scrapeContent(article.link());
                    if (!scrapedContent.isBlank()) {
                        log.debug("Scraped {} chars for article {}", scrapedContent.length(), article.id());
                    }
                }
                
                result.add(new ArticleData(
                    article.id(),
                    article.title(),
                    article.content(),
                    article.authors(),
                    article.link(),
                    article.categoryId(),
                    article.titleZh(),
                    article.contentZh(),
                    scrapedContent,
                    article.score()
                ));
            } catch (Exception e) {
                log.warn("Error scraping article {}: {}", article.id(), e.getMessage());
                result.add(article);
            }
        }
        
        return result;
    }

    private List<ArticleScore> rankBatch(int categoryId, String categoryName,
                                         List<ArticleData> batch,
                                       int batchNum, int totalBatches, String runTimestamp) {
        List<ArticleRanking> bestResult = null;
        int bestSize = 0;
        
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String articlesJson = objectMapper.writeValueAsString(batch);
                String rankingFormat = loadResource("prompt/ranking-format.txt");
                String prompt = loadResource("prompt/ranking-prompt.txt")
                    .replace("{{categoryName}}", categoryName)
                    .replace("{{batchNum}}", String.valueOf(batchNum))
                    .replace("{{totalBatches}}", String.valueOf(totalBatches))
                    .replace("{{jsonFormat}}", rankingFormat)
                    + "\n\nArticles:\n" + articlesJson;

                String response = CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                        .user(prompt)
                        .options(OpenAiChatOptions.builder()
                            .model(reasoningModel)
                            .temperature(rankingTemperature)
                            .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                            .build())
                        .call()
                        .content()
                ).get(timeoutMs, TimeUnit.MILLISECONDS);

                logLlmOutput("ranking_batch" + batchNum, categoryId, response, runTimestamp);

                String json = extractArrayFromObject(extractJsonFromResponse(response));
                BeanOutputConverter<List<ArticleRanking>> converter = new BeanOutputConverter<>(
                    new ParameterizedTypeReference<>() {}
                );
                List<ArticleRanking> rankings = converter.convert(json);
                
                if (rankings != null && !rankings.isEmpty()) {
                    int minAcceptable = (batch.size() * 4) / 5;
                    
                    if (rankings.size() >= batch.size()) {
                        log.info("Ranking attempt {}/{} returned full batch ({} scores) for category {} batch {}", 
                            attempt, MAX_ATTEMPTS, rankings.size(), categoryName, batchNum);
                        return rankings.stream()
                            .filter(Objects::nonNull)
                            .map(r -> new ArticleScore(r.id(), r.score()))
                            .toList();
                    }
                    
                    if (rankings.size() >= minAcceptable) {
                        log.info("Ranking attempt {}/{} returned {} scores for {} articles (>= 80%) for category {} batch {}, accepting", 
                            attempt, MAX_ATTEMPTS, rankings.size(), batch.size(), categoryName, batchNum);
                        return rankings.stream()
                            .filter(Objects::nonNull)
                            .map(r -> new ArticleScore(r.id(), r.score()))
                            .toList();
                    }
                    
                    if (rankings.size() > bestSize) {
                        bestResult = rankings;
                        bestSize = rankings.size();
                    }
                    log.warn("Ranking attempt {}/{} returned {} scores for {} articles in category {} batch {}, retrying", 
                        attempt, MAX_ATTEMPTS, rankings.size(), batch.size(), categoryName, batchNum);
                } else {
                    log.warn("Ranking attempt {}/{} returned empty for category {} batch {}", attempt, MAX_ATTEMPTS, categoryName, batchNum);
                }
            } catch (TimeoutException e) {
                log.warn("Ranking attempt {}/{} timed out for category {} batch {}", attempt, MAX_ATTEMPTS, categoryName, batchNum);
            } catch (RestClientException e) {
                log.warn("Ranking attempt {}/{} API error for category {} batch {}: {}", attempt, MAX_ATTEMPTS, categoryName, batchNum, e.getMessage());
                log.debug("Full RestClientException for category {} batch {}", categoryName, batchNum, e);
            } catch (Exception e) {
                log.warn("Ranking attempt {}/{} failed for category {} batch {}: {}", attempt, MAX_ATTEMPTS, categoryName, batchNum, e.getMessage());
                log.debug("Full exception for category {} batch {}", categoryName, batchNum, e);
            }
        }
        
        if (bestResult != null) {
            log.warn("Using best partial result with {} scores (expected {}) for category {} batch {}", 
                bestSize, batch.size(), categoryName, batchNum);
            return bestResult.stream()
                .filter(Objects::nonNull)
                .map(r -> new ArticleScore(r.id(), r.score()))
                .toList();
        }
        
        return List.of();
    }

    private <T> List<List<T>> splitIntoBatches(List<T> list, int size) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            batches.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return batches;
    }

    private RssNewsSummaryCloud generateSummaries(int categoryId, String categoryName,
                                              List<ArticleData> topArticles, String runTimestamp) {
        RssNewsSummaryCloud bestResult = null;
        int bestSize = 0;
        int minAcceptable = (topArticles.size() * 4) / 5;
        
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String articlesJson = objectMapper.writeValueAsString(topArticles);
                String summaryFormat = loadResource("prompt/summary-format.txt")
                    .replace("{{categoryId}}", String.valueOf(categoryId));
                String prompt = loadResource("prompt/summary-prompt.txt")
                    .replace("{{categoryName}}", categoryName)
                    .replace("{{jsonFormat}}", summaryFormat)
                    + "\n\nArticles:\n" + articlesJson;

                String response = CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                        .user(prompt)
                        .options(OpenAiChatOptions.builder()
                            .model(summaryModel)
                            .temperature(summaryTemperature)
                            .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                            .build())
                        .call()
                        .content()
                ).get(timeoutMs, TimeUnit.MILLISECONDS);

                logLlmOutput("summary", categoryId, response, runTimestamp);

                String json = extractJsonFromResponse(response);
                BeanOutputConverter<RssNewsSummaryCloud> converter = 
                    new BeanOutputConverter<>(RssNewsSummaryCloud.class);
                RssNewsSummaryCloud output = converter.convert(json);
                
                if (output != null && output.getArticlesReport() != null && !output.getArticlesReport().isEmpty()) {
                    int size = output.getArticlesReport().size();
                    
                    if (size >= topArticles.size()) {
                        log.info("Summary attempt {}/{} returned all {} articles for {}", 
                            attempt, MAX_ATTEMPTS, size, categoryName);
                        return output;
                    }
                    
                    if (size >= minAcceptable) {
                        log.info("Summary attempt {}/{} returned {} articles for {} (>= 80%), accepting", 
                            attempt, MAX_ATTEMPTS, size, categoryName);
                        return output;
                    }
                    
                    if (size > bestSize) {
                        bestResult = output;
                        bestSize = size;
                    }
                    log.warn("Summary attempt {}/{} returned {} articles for {} input in category {}, retrying", 
                        attempt, MAX_ATTEMPTS, size, topArticles.size(), categoryName);
                } else {
                    log.warn("Summary attempt {}/{} returned null/empty for {}", attempt, MAX_ATTEMPTS, categoryName);
                }
            } catch (TimeoutException e) {
                log.error("Summary attempt {}/{} timed out after {}ms for {}", attempt, MAX_ATTEMPTS, timeoutMs, categoryName);
            } catch (RestClientException e) {
                log.error("Summary attempt {}/{} API error for {}: {}", attempt, MAX_ATTEMPTS, categoryName, e.getMessage());
                log.debug("Full RestClientException for category {}", categoryName, e);
            } catch (Exception e) {
                log.error("Summary attempt {}/{} failed for {}: {}", attempt, MAX_ATTEMPTS, categoryName, e.getMessage());
                log.debug("Full exception for category {}", categoryName, e);
            }
        }
        
        if (bestResult != null) {
            log.warn("Using best partial summary with {} articles (expected {}) for category {}", 
                bestSize, topArticles.size(), categoryName);
            return bestResult;
        }
        
        return null;
    }

    private String loadResource(String location) {
        try {
            ClassPathResource resource = new ClassPathResource(location);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load resource: " + location, e);
        }
    }

    private String extractJsonFromResponse(String response) {
        String cleaned = response.trim();
        
        int outputStart = cleaned.indexOf("<output>");
        if (outputStart >= 0) {
            int outputEnd = cleaned.indexOf("</output>");
            if (outputEnd > outputStart) {
                return cleaned.substring(outputStart + 8, outputEnd).trim();
            }
        }
        
        int thinkEnd = cleaned.indexOf("</think_more>");
        if (thinkEnd >= 0) {
            cleaned = cleaned.substring(thinkEnd + 12).trim();
        }
        
        int jsonStart = cleaned.indexOf("```json");
        if (jsonStart >= 0) {
            int jsonEnd = cleaned.indexOf("```", jsonStart + 7);
            if (jsonEnd > jsonStart) {
                return cleaned.substring(jsonStart + 7, jsonEnd).trim();
            }
        }
        
        int codeStart = cleaned.indexOf("```");
        if (codeStart >= 0) {
            int codeEnd = cleaned.indexOf("```", codeStart + 3);
            if (codeEnd > codeStart) {
                return cleaned.substring(codeStart + 3, codeEnd).trim();
            }
        }
        
        int arrayStart = cleaned.indexOf('[');
        int objectStart = cleaned.indexOf('{');
        
        if (arrayStart >= 0 && (objectStart < 0 || arrayStart < objectStart)) {
            int arrayEnd = cleaned.lastIndexOf(']');
            if (arrayEnd > arrayStart) {
                String json = sanitizeJson(cleaned.substring(arrayStart, arrayEnd + 1));
                return fixTruncatedJson(json);
            }
            return fixTruncatedJson(cleaned.substring(arrayStart));
        } else if (objectStart >= 0) {
            int objectEnd = cleaned.lastIndexOf('}');
            String json;
            if (objectEnd > objectStart) {
                json = sanitizeJson(cleaned.substring(objectStart, objectEnd + 1));
            } else {
                json = cleaned.substring(objectStart);
            }
            return fixTruncatedJson(json);
        }
        
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            String unquoted = cleaned.substring(1, cleaned.length() - 1);
            return sanitizeJson(unquoted.replace("\\\"", "\"").replace("\\\\", "\\"));
        }
        
        return cleaned;
    }

    private String sanitizeJson(String json) {
        if (json.startsWith("\"{") || json.startsWith("\"[")) {
            try {
                json = objectMapper.readValue(json, String.class);
            } catch (Exception e) {
                json = json.substring(1);
                if (json.endsWith("\"")) {
                    json = json.substring(0, json.length() - 1);
                }
                json = json.replace("\\\"", "\"").replace("\\\\", "\\");
            }
        } else if (json.startsWith("{\"") || json.startsWith("[\"")) {
            json = json.replace("\\\"", "\"").replace("\\\\", "\\");
        }
        
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                if (!isValidJsonEscape(c, i, json)) {
                    result.append('\\');
                }
                result.append(c);
                continue;
            }
            
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
            }
            
            if (inString && c < 32) {
                result.append(escapeControlChar(c));
                continue;
            }
            
            result.append(c);
        }
        
        if (escaped) {
            result.append('\\');
        }
        
        return result.toString();
    }
    
    private boolean isValidJsonEscape(char c, int pos, String json) {
        if (c == '"' || c == '\\' || c == '/' || c == 'b' || c == 'f' || 
            c == 'n' || c == 'r' || c == 't') {
            return true;
        }
        if (c == 'u' && pos + 4 < json.length()) {
            for (int j = 1; j <= 4; j++) {
                char hex = json.charAt(pos + j);
                if (!isHexDigit(hex)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
    
    private String escapeControlChar(char c) {
        return switch (c) {
            case '\t' -> "\\t";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            default -> String.format("\\u%04x", (int) c);
        };
    }

    private String fixTruncatedJson(String json) {
        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;
            if (c == '{') stack.push('{');
            else if (c == '}') {
                if (!stack.isEmpty() && stack.peek() == '{') stack.pop();
            }
            else if (c == '[') stack.push('[');
            else if (c == ']') {
                if (!stack.isEmpty() && stack.peek() == '[') stack.pop();
            }
        }
        
        StringBuilder sb = new StringBuilder(json);
        if (inString) sb.append('"');
        while (!stack.isEmpty()) {
            char ch = stack.pop();
            if (ch == '{') sb.append('}');
            else if (ch == '[') sb.append(']');
        }
        
        return sb.toString();
    }

    private String extractArrayFromObject(String json) {
        json = json.trim();
        
        if (json.startsWith("[{")) {
            int arrayEnd = json.lastIndexOf(']');
            if (arrayEnd > 0) {
                String innerJson = json.substring(1, arrayEnd).trim();
                if (innerJson.startsWith("{") && innerJson.endsWith("}")) {
                    json = innerJson;
                }
            }
        }
        
        if (json.startsWith("[")) {
            return json;
        }
        if (json.startsWith("{")) {
            int arrayStart = json.indexOf('[');
            int arrayEnd = json.lastIndexOf(']');
            if (arrayStart >= 0 && arrayEnd > arrayStart) {
                return json.substring(arrayStart, arrayEnd + 1);
            }
            return "[" + json + "]";
        }
        return "[" + json + "]";
    }

    private String extractReasoningFromResponse(String response) {
        String cleaned = response.trim();
        
        int thinkStart = cleaned.indexOf("<think_more>");
        if (thinkStart >= 0) {
            int thinkEnd = cleaned.indexOf("</think_more>");
            if (thinkEnd > thinkStart) {
                return cleaned.substring(thinkStart + 11, thinkEnd).trim();
            }
        }
        
        return "";
    }

    private void logLlmOutput(String type, int categoryId, String response, String runTimestamp) {
        try {
            Path categoryDir = llmLogDir.resolve(runTimestamp).resolve(String.valueOf(categoryId));
            Files.createDirectories(categoryDir);
            
            String reasoning = extractReasoningFromResponse(response);
            if (!reasoning.isEmpty()) {
                Path reasoningPath = categoryDir.resolve(type + "_reasoning.txt");
                try (FileWriter writer = new FileWriter(reasoningPath.toFile())) {
                    writer.write(reasoning);
                }
                log.debug("Logged reasoning to: {}", reasoningPath);
            }
            
            Path jsonPath = categoryDir.resolve(type + ".json");
            try (FileWriter writer = new FileWriter(jsonPath.toFile())) {
                writer.write(response);
            }
            log.debug("Logged JSON to: {}", jsonPath);
        } catch (Exception e) {
            log.warn("Failed to log LLM output: {}", e.getMessage());
        }
    }

    private record ArticleScore(long id, double score) {}
    
    public record ArticleData(
        long id,
        String title,
        String content,
        List<String> authors,
        String link,
        int categoryId,
        String titleZh,
        String contentZh,
        String scrapedContent,
        Double score
    ) {}
    
    private record BatchContext(
        int categoryId,
        String categoryName,
        List<ArticleData> batch,
        int batchNum,
        int totalBatches,
        String runTimestamp
    ) {}

    private record CategoryArticles(
        int categoryId,
        String categoryName,
        List<ArticleData> articles,
        String runTimestamp
    ) {}
    
    private List<ArticleData> toArticleDataList(List<RssFetchData> fetchData) {
        return fetchData.stream()
            .map(f -> new ArticleData(
                f.getId(),
                f.getTitle(),
                f.getCleanedContentEn() != null ? f.getCleanedContentEn() : "",
                parseAuthors(f.getAuthor()),
                f.getLink(),
                f.getCategoryId(),
                f.getTitleZh() != null ? f.getTitleZh() : "",
                f.getContentZh() != null ? f.getContentZh() : "",
                "",
                null
            ))
            .toList();
    }
    
    private List<ArticleData> toArticleDataListWithScores(List<RssFetchData> fetchData, Map<Long, Double> scoreMap) {
        return fetchData.stream()
            .map(f -> new ArticleData(
                f.getId(),
                f.getTitle(),
                f.getCleanedContentEn() != null ? f.getCleanedContentEn() : "",
                parseAuthors(f.getAuthor()),
                f.getLink(),
                f.getCategoryId(),
                f.getTitleZh() != null ? f.getTitleZh() : "",
                f.getContentZh() != null ? f.getContentZh() : "",
                "",
                scoreMap.get(f.getId())
            ))
            .toList();
    }
    
    private List<String> parseAuthors(String author) {
        if (author == null || author.isBlank()) {
            return List.of();
        }
        return Arrays.stream(author.split(";"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
