package com.tuannt.api.repositories;

import com.tuannt.api.entities.VocabReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Repository
public interface VocabReviewLogRepository extends JpaRepository<VocabReviewLog, Long> {
    long countByUserId(Long userId);

    /** Distinct days with at least one review, newest first. */
    @Query("""
            select distinct l.reviewDay from VocabReviewLog l
            where l.userId = :userId and l.reviewDay >= :since
            order by l.reviewDay desc
            """)
    List<LocalDate> findReviewDays(Long userId, LocalDate since);

    @Modifying
    @Query("delete from VocabReviewLog l where l.wordId = :wordId")
    void deleteByWordId(Long wordId);

    @Modifying
    @Query("delete from VocabReviewLog l where l.userId = :userId")
    void deleteByUserId(Long userId);
}
