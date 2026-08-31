package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.article.ArticleReqDto;
import com.tuannt.api.dtos.article.ArticleResDto;
import com.tuannt.api.services.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Created by tuannt7 on 14/04/2025
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_ARTICLE_PATH)
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;

    // http://localhost:8089/api/v1/article/sources
    @GetMapping("/sources")
    public Map<String, List<String>> getSources() {
        return articleService.getSources();
    }

    // http://localhost:8089/api/v1/article?source=vnexpress&category=tin-moi-nhat
    @GetMapping
    public ArticleResDto getArticle(@Valid ArticleReqDto articleReqDto) {
        return articleService.getArticle(articleReqDto);
    }
}
