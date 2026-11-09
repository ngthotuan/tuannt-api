package com.tuannt.api.configs;

import com.tuannt.api.dtos.article.ArticleCategoryDto;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleConfigTest {

    private static ArticleConfig.ArticleRSS rss(String title) {
        ArticleConfig.ArticleRSS rss = new ArticleConfig.ArticleRSS();
        rss.setUrl("https://example.com/" + title + ".rss");
        rss.setTitle(title);
        return rss;
    }

    @Test
    void categoriesKeepConfigOrderAndCarryTitles() {
        Map<String, ArticleConfig.ArticleRSS> vnexpress = new LinkedHashMap<>();
        vnexpress.put("tin-moi-nhat", rss("Tin mới nhất"));
        vnexpress.put("the-gioi", rss("Thế giới"));
        vnexpress.put("thoi-su", rss("Thời sự"));
        Map<String, Map<String, ArticleConfig.ArticleRSS>> article = new LinkedHashMap<>();
        article.put("vnexpress", vnexpress);
        article.put("dantri", new LinkedHashMap<>(Map.of("trang-chu", rss("Trang chủ"))));

        ArticleConfig config = new ArticleConfig();
        config.setArticle(article);

        Map<String, List<ArticleCategoryDto>> categories = config.getCategories();
        assertThat(categories.keySet()).containsExactly("vnexpress", "dantri");
        assertThat(categories.get("vnexpress")).containsExactly(
                new ArticleCategoryDto("tin-moi-nhat", "Tin mới nhất"),
                new ArticleCategoryDto("the-gioi", "Thế giới"),
                new ArticleCategoryDto("thoi-su", "Thời sự"));
    }
}
