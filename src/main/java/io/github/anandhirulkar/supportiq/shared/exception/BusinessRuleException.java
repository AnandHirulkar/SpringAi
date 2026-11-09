package io.github.anandhirulkar.supportiq.shared.exception;

/** Thrown when a request is well formed but violates a domain invariant or workflow rule. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
