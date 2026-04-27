package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.repo.AiTopArticlesOrchestrator.ArticleData;
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

                if (titleSim > 0.3) {
                    double contentSim = jaccardSimilarity(
                        tokenize(articles.get(i).content()),
                        tokenize(articles.get(j).content())
                    );
                    if (contentSim > 0.5) {
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
            .replaceAll("[^a-z0-9\\s]", " ")
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
}
