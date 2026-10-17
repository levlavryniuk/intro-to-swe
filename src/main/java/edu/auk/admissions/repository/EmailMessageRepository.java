package edu.auk.admissions.repository;

import edu.auk.admissions.domain.EmailMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {

    List<EmailMessage> findByApplicationIdAndDecisionEmailTrueOrderByIdDesc(Long applicationId);

    List<EmailMessage> findTop50ByOrderByIdDesc();
}
