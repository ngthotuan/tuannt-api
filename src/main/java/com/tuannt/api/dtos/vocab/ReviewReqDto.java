package com.tuannt.api.dtos.vocab;

import com.tuannt.api.constants.ReviewGrade;
import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Getter
@Setter
public class ReviewReqDto extends BaseDto {
    @NotNull(message = "wordId is required")
    private Long wordId;

    @NotNull(message = "grade must be AGAIN, HARD, GOOD or EASY")
    private ReviewGrade grade;
}
