package com.tuannt.api.configs;

import com.tuannt.api.entities.AppUser;
import com.tuannt.api.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tuannt7 on 10/11/2026
 * <p>
 * Rejects tokens whose user was deleted or whose "ver" claim is older than the user's
 * token_version. Versions are cached in memory, so a request costs no query; {@link #evict}
 * after a change (once its transaction commits) makes the next request reload it. The cache is
 * per instance, which is fine on the single dyno this runs on.
 */
@Component
@RequiredArgsConstructor
public class TokenVersionValidator implements OAuth2TokenValidator<Jwt> {
    public static final String VERSION_CLAIM = "ver";
    /** Cached for a deleted user, so a revoked token never matches. */
    private static final int MISSING = -1;
    private static final OAuth2Error REVOKED = new OAuth2Error("invalid_token", "The token has been revoked", null);

    private final AppUserRepository userRepository;
    private final Map<Long, Integer> versions = new ConcurrentHashMap<>();

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Long userId;
        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            return OAuth2TokenValidatorResult.failure(REVOKED);
        }
        Number claim = jwt.getClaim(VERSION_CLAIM);
        int tokenVersion = claim == null ? 0 : claim.intValue();
        int current = versions.computeIfAbsent(userId,
                id -> userRepository.findById(id).map(AppUser::getTokenVersion).orElse(MISSING));
        return tokenVersion == current ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(REVOKED);
    }

    /** Drops the cached version once the surrounding transaction commits (at once outside one). */
    public void evict(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    versions.remove(userId);
                }
            });
        } else {
            versions.remove(userId);
        }
    }
}
