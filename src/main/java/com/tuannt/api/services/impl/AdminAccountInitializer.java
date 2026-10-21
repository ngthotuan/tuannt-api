package com.tuannt.api.services.impl;

import com.tuannt.api.configs.AuthConfig;
import com.tuannt.api.constants.UserRole;
import com.tuannt.api.entities.AppUser;
import com.tuannt.api.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Created by tuannt7 on 21/10/2026
 * <p>
 * Accounts can only be created by an admin, so the first admin has to come from config.
 * Create-only: an existing account is never touched, so changing ADMIN_PASSWORD later does
 * not reset the stored password.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {
    private final AuthConfig authConfig;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        String username = authConfig.getAdmin().getUsername();
        String password = authConfig.getAdmin().getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return;
        }
        if (userRepository.existsByUsername(username)) {
            return;
        }
        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);
        log.info("Created bootstrap admin account: {}", username);
    }
}
