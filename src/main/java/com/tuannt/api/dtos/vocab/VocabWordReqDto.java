package com.tuannt.api.dtos.vocab;

import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Getter
@Setter
public class VocabWordReqDto extends BaseDto {
    @NotBlank(message = "en is required")
    @Size(max = 255, message = "en must be at most 255 characters")
    private String en;

    @NotBlank(message = "vi is required")
    @Size(max = 500, message = "vi must be at most 500 characters")
    private String vi;
}
