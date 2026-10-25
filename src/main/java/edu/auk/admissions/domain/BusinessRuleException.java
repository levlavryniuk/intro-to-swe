package edu.auk.admissions.domain;

/** Thrown when a request breaks one of the module's rules, for example applying after the deadline. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
