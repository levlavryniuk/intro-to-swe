package edu.auk.admissions.repository;

import edu.auk.admissions.domain.Lead;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByEmail(String email);
}
