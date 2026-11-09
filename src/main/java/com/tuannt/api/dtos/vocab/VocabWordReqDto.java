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

    /** Optional; blank clears it. Slashes around it are dropped. */
    @Size(max = 100, message = "ipa must be at most 100 characters")
    private String ipa;

    @Size(max = 500, message = "example must be at most 500 characters")
    private String example;

    @Size(max = 60, message = "topic must be at most 60 characters")
    private String topic;
}
