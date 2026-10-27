package com.tuannt.api.repositories;

import com.tuannt.api.entities.VocabSavedWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Repository
public interface VocabSavedWordRepository extends JpaRepository<VocabSavedWord, Long> {
    @Query("select s.wordId from VocabSavedWord s where s.userId = :userId order by s.createdAt desc")
    List<Long> findWordIdsByUserId(Long userId);

    boolean existsByUserIdAndWordId(Long userId, Long wordId);

    long countByUserId(Long userId);

    // Bulk JPQL deletes: the derived deleteBy* variants load every row before removing it.
    @Modifying
    @Query("delete from VocabSavedWord s where s.userId = :userId and s.wordId = :wordId")
    void deleteByUserIdAndWordId(Long userId, Long wordId);

    @Modifying
    @Query("delete from VocabSavedWord s where s.wordId = :wordId")
    void deleteByWordId(Long wordId);

    @Modifying
    @Query("delete from VocabSavedWord s where s.userId = :userId")
    void deleteByUserId(Long userId);
}
