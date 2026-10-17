package edu.auk.admissions.repository;

import edu.auk.admissions.domain.Application;
import edu.auk.admissions.domain.ApplicationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByApplicantId(Long applicantId);

    /** Applications list with optional filters (UC-8). A null filter matches everything. */
    @Query("""
            select a from Application a
            where (:programId is null or a.program.id = :programId)
              and (:status is null or a.status = :status)
            order by a.submittedAt desc, a.id desc
            """)
    List<Application> search(@Param("programId") Long programId, @Param("status") ApplicationStatus status);

    @Query("select a.status, count(a) from Application a where a.status is not null group by a.status")
    List<Object[]> countByStatus();
}
