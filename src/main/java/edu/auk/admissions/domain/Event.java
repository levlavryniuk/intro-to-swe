package edu.auk.admissions.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A recruitment event such as an open day or a webinar (FR-06). */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    private String location;

    private String onlineLink;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("registeredAt ASC")
    private List<EventRegistration> registrations = new ArrayList<>();

    protected Event() {
    }

    public Event(String name, LocalDateTime startsAt, String location, String onlineLink) {
        if (isBlank(location) && isBlank(onlineLink)) {
            throw new BusinessRuleException("Enter a location or an online link.");
        }
        this.name = name;
        this.startsAt = startsAt;
        this.location = isBlank(location) ? null : location.trim();
        this.onlineLink = isBlank(onlineLink) ? null : onlineLink.trim();
    }

    /** UC-2. Refuses a second registration with the same email address. */
    public EventRegistration register(Lead lead, LocalDateTime now) {
        boolean alreadyRegistered = registrations.stream()
                .anyMatch(r -> r.getLead().getEmail().equals(lead.getEmail()));
        if (alreadyRegistered) {
            throw new BusinessRuleException("This email address is already registered for the event.");
        }
        EventRegistration registration = new EventRegistration(this, lead, now);
        registrations.add(registration);
        return registration;
    }

    /** Leads who signed up and accept group email (UC-10). */
    public List<Lead> optedInLeads() {
        return registrations.stream()
                .map(EventRegistration::getLead)
                .filter(Lead::canReceiveGroupEmail)
                .distinct()
                .toList();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public String getLocation() {
        return location;
    }

    public String getOnlineLink() {
        return onlineLink;
    }

    public List<EventRegistration> getRegistrations() {
        return registrations;
    }
}
