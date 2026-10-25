package edu.auk.admissions.web;

import edu.auk.admissions.service.EventService;
import edu.auk.admissions.service.PipelineService;
import edu.auk.admissions.service.ProgramService;
import edu.auk.admissions.web.dto.Requests.DeadlineRequest;
import edu.auk.admissions.web.dto.Requests.EventRequest;
import edu.auk.admissions.web.dto.Requests.ProgramRequest;
import edu.auk.admissions.web.dto.Views.EventView;
import edu.auk.admissions.web.dto.Views.OverviewView;
import edu.auk.admissions.web.dto.Views.ProgramView;
import edu.auk.admissions.web.dto.Views.RegistrationView;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC-12 Manage Events, UC-13 Manage Programs and Deadlines, UC-14 View Pipeline Overview. */
@RestController
@RequestMapping("/api/staff")
public class StaffSetupController {

    private final ProgramService programService;
    private final EventService eventService;
    private final PipelineService pipelineService;
    private final Clock clock;

    public StaffSetupController(ProgramService programService, EventService eventService,
                                PipelineService pipelineService, Clock clock) {
        this.programService = programService;
        this.eventService = eventService;
        this.pipelineService = pipelineService;
        this.clock = clock;
    }

    @GetMapping("/overview")
    OverviewView overview() {
        return OverviewView.of(pipelineService.report());
    }

    @GetMapping("/programs")
    List<ProgramView> programs() {
        LocalDate today = LocalDate.now(clock);
        return programService.list().stream().map(p -> ProgramView.of(p, today)).toList();
    }

    @PostMapping("/programs")
    @ResponseStatus(HttpStatus.CREATED)
    ProgramView addProgram(@Valid @RequestBody ProgramRequest req) {
        return ProgramView.of(programService.add(req.name(), req.deadline()), LocalDate.now(clock));
    }

    @PutMapping("/programs/{id}/deadline")
    ProgramView setDeadline(@PathVariable Long id, @Valid @RequestBody DeadlineRequest req) {
        return ProgramView.of(programService.setDeadline(id, req.deadline()), LocalDate.now(clock));
    }

    @GetMapping("/events")
    List<EventView> events() {
        return eventService.all().stream().map(EventView::of).toList();
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    EventView createEvent(@Valid @RequestBody EventRequest req) {
        return EventView.of(eventService.create(req.name(), req.startsAt(), req.location(), req.onlineLink()));
    }

    @GetMapping("/events/{id}/registrations")
    List<RegistrationView> registrations(@PathVariable Long id) {
        return eventService.get(id).getRegistrations().stream().map(RegistrationView::of).toList();
    }
}
