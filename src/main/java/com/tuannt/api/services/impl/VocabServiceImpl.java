package com.tuannt.api.services.impl;

import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.vocab.AddVocabWordsReqDto;
import com.tuannt.api.dtos.vocab.AddVocabWordsResDto;
import com.tuannt.api.dtos.vocab.PracticeSetResDto;
import com.tuannt.api.dtos.vocab.VocabWordDto;
import com.tuannt.api.dtos.vocab.VocabWordReqDto;
import com.tuannt.api.entities.VocabSavedWord;
import com.tuannt.api.entities.VocabWord;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.exceptions.NotFoundException;
import com.tuannt.api.repositories.VocabReviewLogRepository;
import com.tuannt.api.repositories.VocabReviewRepository;
import com.tuannt.api.repositories.VocabSavedWordRepository;
import com.tuannt.api.repositories.VocabWordRepository;
import com.tuannt.api.services.VocabService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabServiceImpl implements VocabService {
    private final VocabWordRepository wordRepository;
    private final VocabSavedWordRepository savedWordRepository;
    private final VocabReviewRepository reviewRepository;
    private final VocabReviewLogRepository reviewLogRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResDto<VocabWordDto> listWords(int page, int size, String query, boolean savedOnly, Long userId) {
        // Newest first, so a word an admin just added shows up at the top of the first page.
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String pattern = likePattern(query);
        Page<VocabWord> result = savedOnly
                ? wordRepository.searchSaved(userId, pattern, pageable)
                : wordRepository.search(pattern, pageable);
        return PageResDto.of(result, this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeSetResDto randomWords(int size, boolean savedOnly, Long userId) {
        List<VocabWord> sample = savedOnly
                ? wordRepository.randomSavedSample(userId, size)
                : wordRepository.randomSample(size);
        return practiceSet(sample, savedOnly, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeSetResDto sequenceWords(int offset, int size, boolean savedOnly, Long userId) {
        List<VocabWord> words = savedOnly
                ? wordRepository.savedSequence(userId, offset, size)
                : wordRepository.sequence(offset, size);
        return practiceSet(words, savedOnly, userId);
    }

    private PracticeSetResDto practiceSet(List<VocabWord> words, boolean savedOnly, Long userId) {
        long total = savedOnly ? savedWordRepository.countByUserId(userId) : wordRepository.count();
        return PracticeSetResDto.builder()
                .items(words.stream().map(this::toDto).toList())
                .total(total)
                .build();
    }

    @Override
    @Transactional
    public AddVocabWordsResDto addWords(AddVocabWordsReqDto req) {
        // The list is small (hundreds), so checking duplicates in memory is simpler than a query
        // per word, and it also catches repeats inside the same batch.
        Set<String> existing = wordRepository.findAll().stream()
                .map(w -> key(w.getEn()))
                .collect(Collectors.toCollection(HashSet::new));

        Instant now = Instant.now();
        List<VocabWord> toSave = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (VocabWordReqDto item : req.getWords()) {
            String en = item.getEn().trim();
            if (!existing.add(key(en))) {
                skipped.add(en);
                continue;
            }
            VocabWord word = new VocabWord();
            word.setEn(en);
            word.setVi(item.getVi().trim());
            word.setCreatedAt(now);
            toSave.add(word);
        }

        List<VocabWord> saved = wordRepository.saveAll(toSave);
        log.info("Vocab words added: {} skipped: {}", saved.size(), skipped.size());
        return AddVocabWordsResDto.builder()
                .created(saved.stream().map(this::toDto).toList())
                .skipped(skipped)
                .build();
    }

    @Override
    @Transactional
    public VocabWordDto updateWord(Long id, VocabWordReqDto req) {
        VocabWord word = findWord(id);
        String en = req.getEn().trim();
        boolean clashes = wordRepository.findAll().stream()
                .anyMatch(w -> !w.getId().equals(id) && key(w.getEn()).equals(key(en)));
        if (clashes) {
            throw new BadRequestException("Word already exists: " + en);
        }
        word.setEn(en);
        word.setVi(req.getVi().trim());
        log.info("Vocab word updated id: {}", id);
        return toDto(word);
    }

    @Override
    @Transactional
    public void deleteWord(Long id) {
        VocabWord word = findWord(id);
        // No foreign keys: every per-user row that points at the word goes with it.
        savedWordRepository.deleteByWordId(id);
        reviewRepository.deleteByWordId(id);
        reviewLogRepository.deleteByWordId(id);
        wordRepository.delete(word);
        log.info("Vocab word deleted id: {} en: {}", id, word.getEn());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> savedWordIds(Long userId) {
        return savedWordRepository.findWordIdsByUserId(userId);
    }

    @Override
    @Transactional
    public void saveWord(Long userId, Long wordId) {
        findWord(wordId);
        // Idempotent: saving an already-saved word is a no-op, so a double click is harmless.
        if (savedWordRepository.existsByUserIdAndWordId(userId, wordId)) {
            return;
        }
        VocabSavedWord saved = new VocabSavedWord();
        saved.setUserId(userId);
        saved.setWordId(wordId);
        saved.setCreatedAt(Instant.now());
        savedWordRepository.save(saved);
    }

    @Override
    @Transactional
    public void unsaveWord(Long userId, Long wordId) {
        savedWordRepository.deleteByUserIdAndWordId(userId, wordId);
    }

    private VocabWord findWord(Long id) {
        return wordRepository.findById(id).orElseThrow(() -> new NotFoundException(ApiStatus.WORD_NOT_FOUND));
    }

    /** null means "no filter"; LIKE wildcards typed by the user are matched literally. */
    private String likePattern(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** Case-insensitive, stricter than the case-sensitive unique key, so "Career" and "career" collide. */
    private String key(String en) {
        return en.trim().toLowerCase(Locale.ROOT);
    }

    private VocabWordDto toDto(VocabWord word) {
        return VocabWordDto.builder().id(word.getId()).en(word.getEn()).vi(word.getVi()).build();
    }
}
