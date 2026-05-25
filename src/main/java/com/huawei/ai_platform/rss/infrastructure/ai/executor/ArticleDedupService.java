package com.huawei.ai_platform.rss.infrastructure.ai.executor;

import com.huawei.ai_platform.rss.infrastructure.ai.executor.AiTopArticlesOrchestrator.ArticleData;
import com.huawei.ai_platform.rss.model.RssData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class ArticleDedupService {
    public List<ArticleData> deduplicateToTopN(List<ArticleData> allSortedArticles, int targetCount, String categoryName) {
        List<ArticleData> candidates = new ArrayList<>(allSortedArticles.subList(0, Math.min(targetCount, allSortedArticles.size())));
        List<ArticleData> remaining = allSortedArticles.subList(candidates.size(), allSortedArticles.size());

        List<ArticleData> uniqueArticles = deduplicateBySimilarity(candidates);

        while (uniqueArticles.size() < targetCount && !remaining.isEmpty()) {
            int needed = targetCount - uniqueArticles.size();
            List<ArticleData> nextBatch = remaining.subList(0, Math.min(needed, remaining.size()));
            remaining = remaining.subList(nextBatch.size(), remaining.size());

            uniqueArticles.addAll(nextBatch);
            uniqueArticles = deduplicateBySimilarity(uniqueArticles);
            log.info("Dedup round for category {}: added {} candidates -> {} unique (target={})",
                categoryName, nextBatch.size(), uniqueArticles.size(), targetCount);
        }

        return uniqueArticles.stream().limit(targetCount).toList();
    }

    private List<ArticleData> deduplicateBySimilarity(List<ArticleData> articles) {
        if (articles.size() <= 1) {
            return articles;
        }

        Set<Integer> removed = new HashSet<>();
        for (int i = 0; i < articles.size(); i++) {
            if (removed.contains(i)) continue;
            for (int j = i + 1; j < articles.size(); j++) {
                if (removed.contains(j)) continue;

                double titleSim = jaccardSimilarity(
                    tokenize(articles.get(i).title()),
                    tokenize(articles.get(j).title())
                );

                if (titleSim > 0.8) {
                    int toRemove = pickDuplicateToRemove(articles.get(i), articles.get(j), i, j);
                    removed.add(toRemove);
                    log.info("Dedup: title similarity {} between '{}' and '{}' - removing #{}",
                        String.format("%.2f", titleSim), articles.get(i).title(), articles.get(j).title(), toRemove);
                    continue;
                }

                if (titleSim > 0.5) {
                    double contentSim = jaccardSimilarity(
                        tokenize(articles.get(i).content()),
                        tokenize(articles.get(j).content())
                    );
                    if (contentSim > 0.75) {
                        int toRemove = pickDuplicateToRemove(articles.get(i), articles.get(j), i, j);
                        removed.add(toRemove);
                        log.info("Dedup: content similarity {} between '{}' and '{}' - removing #{}",
                            String.format("%.2f", contentSim), articles.get(i).title(), articles.get(j).title(), toRemove);
                    }
                }
            }
        }

        List<ArticleData> result = new ArrayList<>();
        for (int i = 0; i < articles.size(); i++) {
            if (!removed.contains(i)) {
                result.add(articles.get(i));
            }
        }

        if (!removed.isEmpty()) {
            log.info("Dedup removed {} duplicate articles", removed.size());
        }

        return result;
    }

    private int pickDuplicateToRemove(ArticleData a, ArticleData b, int idxA, int idxB) {
        double scoreA = a.score() != null ? a.score() : 0;
        double scoreB = b.score() != null ? b.score() : 0;
        if (scoreB > scoreA) return idxA;
        return idxB;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        String[] words = text.toLowerCase()
            .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
            .split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String w : words) {
            if (w.length() <= 2) continue;
            if (w.endsWith("ing")) w = w.substring(0, w.length() - 3);
            else if (w.endsWith("ed")) w = w.substring(0, w.length() - 2);
            else if (w.endsWith("ly")) w = w.substring(0, w.length() - 2);
            else if (w.endsWith("s") && !w.endsWith("ss")) w = w.substring(0, w.length() - 1);
            if (!w.isEmpty()) tokens.add(w);
        }
        return tokens;
    }

    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        Set<String> union = new HashSet<>(a);
        union.addAll(b);

        return (double) intersection.size() / union.size();
    }

    /**
     * Finds duplicate article IDs using inverted index for O(N×C) instead of O(N²).
     * Returns map of duplicateArticleId → originalArticleId it duplicates.
     */
    public Map<Long, Long> findDuplicates(List<RssData> articles) {
        if (articles.size() <= 1) return Map.of();

        Map<Long, Long> duplicateToOriginal = new HashMap<>();
        Map<String, List<RssData>> tokenIndex = new HashMap<>();

        for (RssData article : articles) {
            if (duplicateToOriginal.containsKey(article.getArticleId())) continue;

            Set<String> titleTokens = tokenize(article.getArticleTitleEn());
            Set<String> contentTokens = tokenize(article.getArticleContent());

            RssData matchedOriginal = null;

            // Find candidates via title token overlap
            Map<RssData, Integer> candidateOverlap = new HashMap<>();
            for (String token : titleTokens) {
                for (RssData candidate : tokenIndex.getOrDefault(token, List.of())) {
                    if (candidate.getArticleId() == article.getArticleId()) continue;
                    candidateOverlap.merge(candidate, 1, Integer::sum);
                }
            }

            // Only compute full Jaccard for candidates with shared title tokens
            for (RssData candidate : candidateOverlap.keySet()) {
                if (candidate.getArticleId() == article.getArticleId()) continue;
                if (duplicateToOriginal.containsKey(candidate.getArticleId())) continue;

                double titleSim = jaccardSimilarity(titleTokens, tokenize(candidate.getArticleTitleEn()));

                if (titleSim > 0.85) {
                    matchedOriginal = candidate;
                    break;
                }

                if (titleSim > 0.6) {
                    double contentSim = jaccardSimilarity(contentTokens, tokenize(candidate.getArticleContent()));
                    if (contentSim > 0.85) {
                        matchedOriginal = candidate;
                        break;
                    }
                }
            }

            if (matchedOriginal != null) {
                duplicateToOriginal.put(article.getArticleId(), matchedOriginal.getArticleId());
            } else {
                for (String token : titleTokens) {
                    tokenIndex.computeIfAbsent(token, k -> new ArrayList<>()).add(article);
                }
            }
        }

        return duplicateToOriginal;
    }
}
