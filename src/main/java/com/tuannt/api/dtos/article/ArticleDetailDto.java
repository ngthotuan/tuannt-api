package com.tuannt.api.dtos.article;

import com.tuannt.api.dtos.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Created by tuannt7 on 14/04/2025
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDetailDto extends BaseDto {
    private String title;
    private String link;
    private String image;
    private String description;
    private Date publishedDate;
}
