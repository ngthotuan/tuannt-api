package com.tuannt.api.services;

import com.tuannt.api.dtos.quiz.QuizAttemptReqDto;
import com.tuannt.api.dtos.quiz.QuizHistoryResDto;

/**
 * Created by tuannt7 on 01/09/2026
 */
public interface QuizAttemptService {
    Long record(QuizAttemptReqDto req, String ip);

    QuizHistoryResDto history(String deviceId, int limit);
}
