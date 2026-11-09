package com.tuannt.api.configs;

import com.tuannt.api.dtos.article.ArticleCategoryDto;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by tuannt7 on 14/04/2025
 */

@Data
@ConfigurationProperties(prefix = "config")
public class ArticleConfig {
    private Map<String, Map<String, ArticleRSS>> article;

    public Map<String, List<String>> getSources() {
        return article.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue().keySet())));
    }

    /** Categories with their display titles, in the order they are configured. */
    public Map<String, List<ArticleCategoryDto>> getCategories() {
        Map<String, List<ArticleCategoryDto>> result = new LinkedHashMap<>();
        article.forEach((source, categories) -> result.put(source, categories.entrySet().stream()
                .map(entry -> new ArticleCategoryDto(entry.getKey(), entry.getValue().getTitle()))
                .toList()));
        return result;
    }

    public Optional<ArticleRSS> getArticle(String source, String category) {
        return Optional.ofNullable(article.get(source))
                .map(articles -> articles.get(category));
    }

    @Data
    public static class ArticleRSS {
        private String url;
        private String title;
    }
}
