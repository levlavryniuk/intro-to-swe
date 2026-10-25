package edu.auk.admissions.repository;

import edu.auk.admissions.domain.Program;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    List<Program> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
