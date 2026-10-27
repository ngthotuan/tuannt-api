package com.tuannt.api.dtos.post;

import com.tuannt.api.dtos.BaseDto;
import lombok.Builder;
import lombok.Getter;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Getter
@Builder
public class TagCountDto extends BaseDto {
    private String tag;
    /** Number of published posts carrying the tag. */
    private long count;
}
