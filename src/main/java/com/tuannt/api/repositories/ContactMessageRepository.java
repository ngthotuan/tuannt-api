package com.tuannt.api.repositories;

import com.tuannt.api.entities.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}
