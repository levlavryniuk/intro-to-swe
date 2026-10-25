package edu.auk.admissions.repository;

import edu.auk.admissions.domain.Inquiry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByOrderBySubmittedAtDescIdDesc();

    List<Inquiry> findByProgramIdOrderBySubmittedAtDescIdDesc(Long programId);
}
