package com.tuannt.api.dtos;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Created by tuannt7 on 21/10/2026
 * <p>
 * Our own shape rather than Spring's Page JSON, which is verbose and not guaranteed stable.
 */
@Getter
@Builder
public class PageResDto<T> extends BaseDto {
    private List<T> items;
    /** Zero-based. */
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public static <E, T> PageResDto<T> of(Page<E> page, Function<E, T> mapper) {
        return PageResDto.<T>builder()
                .items(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
