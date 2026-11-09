package io.github.anandhirulkar.supportiq.shared.exception;

/**
 * The model provider is unreachable, unconfigured or the circuit breaker is open.
 * Surfaced as HTTP 503 so clients can distinguish an infrastructure problem from a bad request.
 */
public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
