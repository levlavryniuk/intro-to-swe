package edu.auk.admissions.web.dto;

import edu.auk.admissions.domain.ApplicationStatus;
import edu.auk.admissions.domain.InterestLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Request bodies. Bean Validation checks them on the server before anything is saved (NFR-06). */
public final class Requests {

    private Requests() {
    }

    public record InquiryRequest(
            @NotBlank(message = "Enter your name.") @Size(max = 200) String name,
            @NotBlank(message = "Enter your email address.") @Email(message = "Enter a valid email address.") String email,
            @NotNull(message = "Choose a program.") Long programId,
            @Size(max = 2000, message = "Keep the question under 2000 characters.") String question,
            boolean promotionalOptIn) {
    }

    public record RegistrationRequest(
            @NotBlank(message = "Enter your name.") @Size(max = 200) String name,
            @NotBlank(message = "Enter your email address.") @Email(message = "Enter a valid email address.") String email,
            boolean promotionalOptIn) {
    }

    public record SignUpRequest(
            @NotBlank(message = "Enter your name.") @Size(max = 200) String name,
            @NotBlank(message = "Enter your email address.") @Email(message = "Enter a valid email address.") String email,
            @NotBlank(message = "Choose a password.") @Size(min = 8, message = "Use at least 8 characters.") String password) {
    }

    /**
     * Fields of the application form (UC-3, UC-4). Sent as multipart form data together with the files.
     * The program is chosen only when submitting, so it is a separate parameter there.
     */
    public record ApplicationForm(
            @NotBlank(message = "Enter your phone number.") @Size(max = 40) String phone,
            @NotBlank(message = "Enter your address.") @Size(max = 300) String address,
            @NotBlank(message = "Enter the last school you attended.") @Size(max = 200) String lastSchool,
            @NotNull(message = "Enter your graduation year.")
            @Min(value = 1950, message = "Enter a graduation year from 1950 on.")
            @Max(value = 2100, message = "Enter a valid graduation year.") Integer graduationYear,
            @NotNull(message = "Enter your grade average.")
            @DecimalMin(value = "0.0", message = "The grade average cannot be negative.")
            @DecimalMax(value = "100.0", message = "The grade average cannot be above 100.") Double gradeAverage) {
    }

    public record InterestRequest(@NotNull(message = "Choose High, Medium or Low.") InterestLevel interest) {
    }

    public record DecisionRequest(
            @NotNull(message = "Choose a status.") ApplicationStatus status,
            @Size(max = 1000) String condition) {
    }

    public record EmailRequest(
            @NotBlank(message = "Enter a subject.") @Size(max = 200) String subject,
            @NotBlank(message = "Enter a message.") @Size(max = 10000) String body) {
    }

    public record GroupEmailRequest(
            Long programId,
            Long eventId,
            @NotBlank(message = "Enter a subject.") @Size(max = 200) String subject,
            @NotBlank(message = "Enter a message.") @Size(max = 10000) String body) {
    }

    public record ProgramRequest(
            @NotBlank(message = "Enter the program name.") @Size(max = 200) String name,
            LocalDate deadline) {
    }

    public record DeadlineRequest(@NotNull(message = "Enter a date.") LocalDate deadline) {
    }

    public record EventRequest(
            @NotBlank(message = "Enter the event name.") @Size(max = 200) String name,
            @NotNull(message = "Enter the date and time.") LocalDateTime startsAt,
            @Size(max = 300) String location,
            @Size(max = 500) String onlineLink) {
    }
}
