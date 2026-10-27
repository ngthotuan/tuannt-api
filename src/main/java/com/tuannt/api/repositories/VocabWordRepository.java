package com.tuannt.api.repositories;

import com.tuannt.api.entities.VocabWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Repository
public interface VocabWordRepository extends JpaRepository<VocabWord, Long> {
    // lower() on both sides: TiDB's default utf8mb4_bin collation makes LIKE case-sensitive.
    // :pattern is null for "no search", otherwise an already-escaped, lower-cased %term%.
    @Query("""
            select w from VocabWord w
            where (:pattern is null or lower(w.en) like :pattern escape '\\' or lower(w.vi) like :pattern escape '\\')
            """)
    Page<VocabWord> search(String pattern, Pageable pageable);

    @Query("""
            select w from VocabWord w
            where exists (select 1 from VocabSavedWord s where s.wordId = w.id and s.userId = :userId)
              and (:pattern is null or lower(w.en) like :pattern escape '\\' or lower(w.vi) like :pattern escape '\\')
            """)
    Page<VocabWord> searchSaved(Long userId, String pattern, Pageable pageable);

    // Sequential practice walks the list oldest first (the original sheet order) from any
    // position, which page numbers can't express, hence LIMIT/OFFSET.
    @Query(value = "SELECT * FROM vocab_word ORDER BY id LIMIT :size OFFSET :offset", nativeQuery = true)
    List<VocabWord> sequence(int offset, int size);

    @Query(value = """
            SELECT w.* FROM vocab_word w
            JOIN vocab_saved_word s ON s.word_id = w.id
            WHERE s.user_id = :userId
            ORDER BY w.id LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<VocabWord> savedSequence(Long userId, int offset, int size);

    // ORDER BY RAND() scans the whole table, which is fine at hundreds or a few thousand rows.
    @Query(value = "SELECT * FROM vocab_word ORDER BY RAND() LIMIT :size", nativeQuery = true)
    List<VocabWord> randomSample(int size);

    @Query(value = """
            SELECT w.* FROM vocab_word w
            JOIN vocab_saved_word s ON s.word_id = w.id
            WHERE s.user_id = :userId
            ORDER BY RAND() LIMIT :size
            """, nativeQuery = true)
    List<VocabWord> randomSavedSample(Long userId, int size);

    // Words the user has never reviewed, oldest first like sequential practice. savedOnly=false
    // skips the saved-word filter, so one query serves both review queue scopes.
    @Query("""
            select w from VocabWord w
            where not exists (select 1 from VocabReview r where r.wordId = w.id and r.userId = :userId)
              and (:savedOnly = false
                   or exists (select 1 from VocabSavedWord s where s.wordId = w.id and s.userId = :userId))
            order by w.id
            """)
    List<VocabWord> findUnreviewed(Long userId, boolean savedOnly, Pageable pageable);

    @Query("""
            select count(w) from VocabWord w
            where not exists (select 1 from VocabReview r where r.wordId = w.id and r.userId = :userId)
              and (:savedOnly = false
                   or exists (select 1 from VocabSavedWord s where s.wordId = w.id and s.userId = :userId))
            """)
    long countUnreviewed(Long userId, boolean savedOnly);
}
