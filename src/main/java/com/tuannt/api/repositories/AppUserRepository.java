package com.tuannt.api.repositories;

import com.tuannt.api.constants.UserRole;
import com.tuannt.api.entities.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    // SELECT ... FOR UPDATE on every admin row, so two concurrent demotions or deletions cannot
    // both see "another admin exists" and leave none.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.role = :role")
    List<AppUser> lockByRole(UserRole role);
}
