package edu.auk.admissions.web;

import edu.auk.admissions.domain.BusinessRuleException;
import edu.auk.admissions.service.NotFoundException;
import edu.auk.admissions.web.dto.Views.ErrorView;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Turns errors into JSON messages that say how to fix the problem (NFR-04, NFR-06). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorView invalidFields(BindException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getFieldErrors().forEach(err -> fields.putIfAbsent(err.getField(),
                err.isBindingFailure() ? "Enter a valid value." : err.getDefaultMessage()));
        return new ErrorView("Check the highlighted fields.", fields);
    }

    @ExceptionHandler(FieldValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorView invalidField(FieldValidationException e) {
        return new ErrorView("Check the highlighted fields.", Map.of(e.getField(), e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorView unreadable(Exception e) {
        return new ErrorView("Some of the values are not in the expected format.");
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorView ruleBroken(BusinessRuleException e) {
        return new ErrorView(e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorView conflict(DataIntegrityViolationException e) {
        return new ErrorView("This conflicts with data that already exists. Reload the page and try again.");
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorView notFound(NotFoundException e) {
        return new ErrorView(e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    ErrorView tooLarge(MaxUploadSizeExceededException e) {
        return new ErrorView("Each file must be a PDF of at most 15 MB.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ErrorView forbidden(AccessDeniedException e) {
        return new ErrorView(e.getMessage());
    }
}
