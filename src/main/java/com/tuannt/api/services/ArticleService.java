package com.tuannt.api.services;

import com.tuannt.api.dtos.article.ArticleReqDto;
import com.tuannt.api.dtos.article.ArticleResDto;

import java.util.List;
import java.util.Map;

/**
 * Created by tuannt7 on 14/04/2025
 */

public interface ArticleService {
    Map<String, List<String>> getSources();

    ArticleResDto getArticle(ArticleReqDto articleReqDto);
}
