package edu.auk.admissions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/** Records that one lead registered for one event, and when. */
@Entity
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Event event;

    @ManyToOne(optional = false)
    private Lead lead;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    protected EventRegistration() {
    }

    EventRegistration(Event event, Lead lead, LocalDateTime registeredAt) {
        this.event = event;
        this.lead = lead;
        this.registeredAt = registeredAt;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public Lead getLead() {
        return lead;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
}
