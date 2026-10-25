package edu.auk.admissions.service;

import edu.auk.admissions.domain.BusinessRuleException;
import edu.auk.admissions.domain.Program;
import edu.auk.admissions.repository.ProgramRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-13 Manage Programs and Deadlines. */
@Service
public class ProgramService {

    private final ProgramRepository programs;

    public ProgramService(ProgramRepository programs) {
        this.programs = programs;
    }

    @Transactional(readOnly = true)
    public List<Program> list() {
        return programs.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Program get(Long id) {
        return programs.findById(id).orElseThrow(() -> new NotFoundException("Program not found."));
    }

    @Transactional
    public Program add(String name, LocalDate deadline) {
        String trimmed = name.trim();
        if (programs.existsByNameIgnoreCase(trimmed)) {
            throw new BusinessRuleException("A program named " + trimmed + " already exists.");
        }
        return programs.save(new Program(trimmed, deadline));
    }

    @Transactional
    public Program setDeadline(Long id, LocalDate deadline) {
        Program program = get(id);
        program.changeDeadline(deadline);
        return program;
    }
}
