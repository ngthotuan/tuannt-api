package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.vocab.AddVocabWordsReqDto;
import com.tuannt.api.dtos.vocab.AddVocabWordsResDto;
import com.tuannt.api.dtos.vocab.VocabWordDto;
import com.tuannt.api.dtos.vocab.VocabWordReqDto;
import com.tuannt.api.services.VocabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 * <p>
 * Reading the word list is public. Writes to /words are ADMIN only and /saved needs any
 * logged-in user; both rules live in SecurityConfig.
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_VOCAB_PATH)
@RequiredArgsConstructor
public class VocabController {
    private final VocabService vocabService;

    @GetMapping("/words")
    public List<VocabWordDto> listWords() {
        return vocabService.listWords();
    }

    @PostMapping("/words")
    @ResponseStatus(HttpStatus.CREATED)
    public AddVocabWordsResDto addWords(@Valid @RequestBody AddVocabWordsReqDto req) {
        return vocabService.addWords(req);
    }

    @PutMapping("/words/{id}")
    public VocabWordDto updateWord(@PathVariable Long id, @Valid @RequestBody VocabWordReqDto req) {
        return vocabService.updateWord(id, req);
    }

    @DeleteMapping("/words/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWord(@PathVariable Long id) {
        vocabService.deleteWord(id);
    }

    @GetMapping("/saved")
    public List<Long> savedWordIds(@AuthenticationPrincipal Jwt jwt) {
        return vocabService.savedWordIds(userId(jwt));
    }

    @PutMapping("/saved/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveWord(@PathVariable Long wordId, @AuthenticationPrincipal Jwt jwt) {
        vocabService.saveWord(userId(jwt), wordId);
    }

    @DeleteMapping("/saved/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsaveWord(@PathVariable Long wordId, @AuthenticationPrincipal Jwt jwt) {
        vocabService.unsaveWord(userId(jwt), wordId);
    }

    /** The token subject is the app_user id, set at login. */
    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
