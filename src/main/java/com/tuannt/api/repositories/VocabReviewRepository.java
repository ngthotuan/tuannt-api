package com.tuannt.api.repositories;

import com.tuannt.api.entities.VocabReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Repository
public interface VocabReviewRepository extends JpaRepository<VocabReview, Long> {
    Optional<VocabReview> findByUserIdAndWordId(Long userId, Long wordId);

    // savedOnly=false skips the saved-word filter, so one query serves both queue scopes.
    @Query("""
            select r from VocabReview r
            where r.userId = :userId and r.dueAt <= :now
              and (:savedOnly = false
                   or exists (select 1 from VocabSavedWord s where s.userId = r.userId and s.wordId = r.wordId))
            order by r.dueAt, r.id
            """)
    List<VocabReview> findDue(Long userId, Instant now, boolean savedOnly, Pageable pageable);

    @Query("""
            select count(r) from VocabReview r
            where r.userId = :userId and r.dueAt <= :now
              and (:savedOnly = false
                   or exists (select 1 from VocabSavedWord s where s.userId = r.userId and s.wordId = r.wordId))
            """)
    long countDue(Long userId, Instant now, boolean savedOnly);

    long countByUserIdAndDueAtBefore(Long userId, Instant before);

    long countByUserIdAndRepetitionsGreaterThanEqual(Long userId, int repetitions);

    long countByUserIdAndIntervalDaysGreaterThanEqual(Long userId, int intervalDays);

    // Bulk JPQL deletes, like VocabSavedWordRepository: derived deleteBy* would load each row first.
    @Modifying
    @Query("delete from VocabReview r where r.wordId = :wordId")
    void deleteByWordId(Long wordId);

    @Modifying
    @Query("delete from VocabReview r where r.userId = :userId")
    void deleteByUserId(Long userId);
}
