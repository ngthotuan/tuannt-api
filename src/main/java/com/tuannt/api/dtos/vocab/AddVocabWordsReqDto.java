package com.tuannt.api.dtos.vocab;

import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 */

/** A batch so admins can paste many lines at once, the way they used to fill the sheet. */
@Getter
@Setter
public class AddVocabWordsReqDto extends BaseDto {
    @Valid
    @NotEmpty(message = "words is required")
    @Size(max = 500, message = "at most 500 words per request")
    private List<VocabWordReqDto> words = new ArrayList<>();
}
