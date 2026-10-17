package edu.auk.admissions.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalAccountRepository extends JpaRepository<LocalAccount, Long> {

    Optional<LocalAccount> findByUserEmailIgnoreCase(String email);
}
