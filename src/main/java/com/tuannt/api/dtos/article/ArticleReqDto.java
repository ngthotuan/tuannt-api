package com.tuannt.api.dtos.article;

import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by tuannt7 on 14/04/2025
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReqDto extends BaseDto {
    @NotBlank(message = "source is required")
    private String source;
    @NotBlank(message = "category is required")
    private String category;
}
