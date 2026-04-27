package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArticleDedupService {
    public List<AiTopArticlesOrchestrator.ArticleData> deduplicateToTopN(List<AiTopArticlesOrchestrator.ArticleData> allSortedArticles, int topArticlesCount, String s) {
        return null;
    }
}
