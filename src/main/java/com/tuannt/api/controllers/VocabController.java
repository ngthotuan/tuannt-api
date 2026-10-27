package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.vocab.AddVocabWordsReqDto;
import com.tuannt.api.dtos.vocab.AddVocabWordsResDto;
import com.tuannt.api.dtos.vocab.PracticeSetResDto;
import com.tuannt.api.dtos.vocab.VocabWordDto;
import com.tuannt.api.dtos.vocab.VocabWordReqDto;
import com.tuannt.api.exceptions.UnauthorizedException;
import com.tuannt.api.services.VocabService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 * <p>
 * Reading the word list is public. Writes to /words are ADMIN only and /saved needs any
 * logged-in user; both rules live in SecurityConfig. The read endpoints also accept saved=true,
 * which needs a token too but is checked here because the path itself stays public.
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_VOCAB_PATH)
@RequiredArgsConstructor
public class VocabController {
    private final VocabService vocabService;

    @GetMapping("/words")
    public PageResDto<VocabWordDto> listWords(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be at least 0")
            // page * size must stay an int offset, or Spring Data throws and it surfaces as a 500.
            @Max(value = 100_000, message = "page must be at most 100000") int page,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size,
            @RequestParam(required = false) @Size(max = 100, message = "q must be at most 100 characters") String q,
            @RequestParam(defaultValue = "false") boolean saved,
            @AuthenticationPrincipal Jwt jwt) {
        return vocabService.listWords(page, size, q, saved, saved ? requireUserId(jwt) : null);
    }

    /** A random practice set, so the client never has to download the whole list to shuffle it. */
    @GetMapping("/words/random")
    public PracticeSetResDto randomWords(
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 200, message = "size must be at most 200") int size,
            @RequestParam(defaultValue = "false") boolean saved,
            @AuthenticationPrincipal Jwt jwt) {
        return vocabService.randomWords(size, saved, saved ? requireUserId(jwt) : null);
    }

    /** The next practice set in list order (oldest first), from a zero-based offset. */
    @GetMapping("/words/sequence")
    public PracticeSetResDto sequenceWords(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "offset must be at least 0") int offset,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 200, message = "size must be at most 200") int size,
            @RequestParam(defaultValue = "false") boolean saved,
            @AuthenticationPrincipal Jwt jwt) {
        return vocabService.sequenceWords(offset, size, saved, saved ? requireUserId(jwt) : null);
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

    /** Anonymous callers resolve to a null Jwt on these public paths. */
    private Long requireUserId(Jwt jwt) {
        if (jwt == null) {
            throw new UnauthorizedException(ApiStatus.UNAUTHORIZED);
        }
        return userId(jwt);
    }

    /** The token subject is the app_user id, set at login. */
    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
