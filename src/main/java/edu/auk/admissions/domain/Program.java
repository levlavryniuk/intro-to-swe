package edu.auk.admissions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Something a student can apply to, with its application deadline (FR-08). */
@Entity
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Last day applications and edits are accepted. Null until staff set it. */
    private LocalDate deadline;

    @OneToMany(mappedBy = "program")
    private List<Inquiry> inquiries = new ArrayList<>();

    protected Program() {
    }

    public Program(String name, LocalDate deadline) {
        this.name = name;
        this.deadline = deadline;
    }

    /**
     * The single place that decides whether new applications and edits are accepted.
     * They are refused from the end of the deadline date, and while no deadline is set.
     */
    public boolean isOpen(LocalDate today) {
        return deadline != null && !today.isAfter(deadline);
    }

    public void changeDeadline(LocalDate date) {
        this.deadline = date;
    }

    /** Leads who asked about this program and accept group email (UC-10). */
    public List<Lead> optedInLeads() {
        Set<Lead> leads = new LinkedHashSet<>();
        for (Inquiry inquiry : inquiries) {
            if (inquiry.getLead().canReceiveGroupEmail()) {
                leads.add(inquiry.getLead());
            }
        }
        return new ArrayList<>(leads);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDeadline() {
        return deadline;
    }
}
