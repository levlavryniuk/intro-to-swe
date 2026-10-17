package edu.auk.admissions.service;

import edu.auk.admissions.domain.Event;
import edu.auk.admissions.domain.EventRegistration;
import edu.auk.admissions.domain.Lead;
import edu.auk.admissions.repository.EventRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-2 Register for Event and UC-12 Manage Events. */
@Service
public class EventService {

    private final EventRepository events;
    private final LeadService leadService;
    private final Clock clock;

    public EventService(EventRepository events, LeadService leadService, Clock clock) {
        this.events = events;
        this.leadService = leadService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Event> upcoming() {
        return events.findByStartsAtAfterOrderByStartsAtAsc(LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public List<Event> all() {
        return events.findAllByOrderByStartsAtAsc();
    }

    @Transactional(readOnly = true)
    public Event get(Long id) {
        return events.findById(id).orElseThrow(() -> new NotFoundException("Event not found."));
    }

    @Transactional
    public Event create(String name, LocalDateTime startsAt, String location, String onlineLink) {
        return events.save(new Event(name.trim(), startsAt, location, onlineLink));
    }

    @Transactional
    public EventRegistration register(Long eventId, String name, String email, boolean promotionalOptIn) {
        Event event = get(eventId);
        Lead lead = leadService.findOrCreate(name, email, promotionalOptIn);
        EventRegistration registration = event.register(lead, LocalDateTime.now(clock));
        events.flush();
        return registration;
    }
}
