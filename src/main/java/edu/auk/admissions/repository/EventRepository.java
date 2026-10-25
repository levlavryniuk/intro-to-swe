package edu.auk.admissions.repository;

import edu.auk.admissions.domain.Event;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStartsAtAfterOrderByStartsAtAsc(LocalDateTime after);

    List<Event> findAllByOrderByStartsAtAsc();
}
