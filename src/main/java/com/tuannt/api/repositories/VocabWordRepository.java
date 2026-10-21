package com.tuannt.api.repositories;

import com.tuannt.api.entities.VocabWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Repository
public interface VocabWordRepository extends JpaRepository<VocabWord, Long> {
    List<VocabWord> findAllByOrderByIdAsc();
}
