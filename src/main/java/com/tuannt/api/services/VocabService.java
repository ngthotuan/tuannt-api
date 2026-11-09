package com.tuannt.api.services;

import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.vocab.AddVocabWordsReqDto;
import com.tuannt.api.dtos.vocab.AddVocabWordsResDto;
import com.tuannt.api.dtos.vocab.PracticeSetResDto;
import com.tuannt.api.dtos.vocab.TopicCountDto;
import com.tuannt.api.dtos.vocab.VocabWordDto;
import com.tuannt.api.dtos.vocab.VocabWordReqDto;

import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 */
public interface VocabService {
    /** userId is required when savedOnly is true, otherwise ignored. */
    PageResDto<VocabWordDto> listWords(int page, int size, String query, String topic, boolean savedOnly, Long userId);

    PracticeSetResDto randomWords(int size, String topic, boolean savedOnly, Long userId);

    /** Oldest first, starting at a zero-based offset. */
    PracticeSetResDto sequenceWords(int offset, int size, String topic, boolean savedOnly, Long userId);

    /** Topics in use, alphabetical, with word counts. */
    List<TopicCountDto> topics();

    AddVocabWordsResDto addWords(AddVocabWordsReqDto req);

    VocabWordDto updateWord(Long id, VocabWordReqDto req);

    void deleteWord(Long id);

    List<Long> savedWordIds(Long userId);

    void saveWord(Long userId, Long wordId);

    void unsaveWord(Long userId, Long wordId);
}
