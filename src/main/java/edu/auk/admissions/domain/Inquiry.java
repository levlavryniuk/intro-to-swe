package edu.auk.admissions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/** One message from a lead about one program (FR-01). */
@Entity
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Lead lead;

    @ManyToOne(optional = false)
    private Program program;

    @Column(length = 2000)
    private String question;

    @Enumerated(EnumType.STRING)
    private InterestLevel interest;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    protected Inquiry() {
    }

    public Inquiry(Lead lead, Program program, String question, LocalDateTime submittedAt) {
        this.lead = lead;
        this.program = program;
        this.question = question;
        this.submittedAt = submittedAt;
    }

    public void setInterest(InterestLevel level) {
        this.interest = level;
    }

    public Long getId() {
        return id;
    }

    public Lead getLead() {
        return lead;
    }

    public Program getProgram() {
        return program;
    }

    public String getQuestion() {
        return question;
    }

    public InterestLevel getInterest() {
        return interest;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
