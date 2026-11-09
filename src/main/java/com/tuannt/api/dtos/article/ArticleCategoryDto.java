package com.tuannt.api.dtos.article;

/** A feed category: its key (used in `?category=`) and its display title. */
public record ArticleCategoryDto(String key, String title) {
}
