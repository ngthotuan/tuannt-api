package com.tuannt.api.services;

import com.tuannt.api.dtos.vocab.AddVocabWordsReqDto;
import com.tuannt.api.dtos.vocab.AddVocabWordsResDto;
import com.tuannt.api.dtos.vocab.VocabWordDto;
import com.tuannt.api.dtos.vocab.VocabWordReqDto;

import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 */
public interface VocabService {
    List<VocabWordDto> listWords();

    AddVocabWordsResDto addWords(AddVocabWordsReqDto req);

    VocabWordDto updateWord(Long id, VocabWordReqDto req);

    void deleteWord(Long id);

    List<Long> savedWordIds(Long userId);

    void saveWord(Long userId, Long wordId);

    void unsaveWord(Long userId, Long wordId);
}
