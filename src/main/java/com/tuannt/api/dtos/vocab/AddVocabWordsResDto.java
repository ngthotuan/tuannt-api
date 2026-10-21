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
public class AddVocabWordsResDto extends BaseDto {
    private List<VocabWordDto> created;
    /** English terms that already existed (or repeated within the batch) and were not added. */
    private List<String> skipped;
}
