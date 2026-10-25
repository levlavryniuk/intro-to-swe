package edu.auk.admissions.web;

/** A single invalid form field that Bean Validation cannot check, reported like the others (400). */
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
