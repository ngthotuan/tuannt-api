package com.tuannt.api.repositories;

import com.tuannt.api.entities.QuizAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /** Entity graph avoids an N+1 query when rendering wrong answers for each attempt. */
    @EntityGraph(attributePaths = "wrongAnswers")
    List<QuizAttempt> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);
}
