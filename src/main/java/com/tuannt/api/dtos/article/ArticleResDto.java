package com.tuannt.api.dtos.article;

import com.tuannt.api.dtos.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * Created by tuannt7 on 14/04/2025
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResDto extends BaseDto {
    private String title;
    private Date update;
    private String source;
    private String category;
    private int size;
    private List<ArticleDetailDto> feeds;
}
