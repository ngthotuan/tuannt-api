package com.tuannt.api.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * The four answer buttons of a review, with the SM-2 quality (0-5) each one stands for.
 */
@Getter
@AllArgsConstructor
public enum ReviewGrade {
    AGAIN(1),
    HARD(3),
    GOOD(4),
    EASY(5);

    private final int quality;
}
