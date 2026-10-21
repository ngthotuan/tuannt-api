package com.tuannt.api.dtos.vocab;

import com.tuannt.api.dtos.BaseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Getter
@Builder
public class PracticeSetResDto extends BaseDto {
    private List<VocabWordDto> items;
    /** Size of the pool the set was drawn from, so the UI can show "20 of 68". */
    private long total;
}
