package com.tuannt.api.dtos.vocab;

import com.tuannt.api.dtos.BaseDto;
import lombok.Builder;
import lombok.Getter;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Getter
@Builder
public class VocabWordDto extends BaseDto {
    private Long id;
    private String en;
    private String vi;
}
