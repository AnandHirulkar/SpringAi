package io.github.anandhirulkar.supportiq.shared.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import io.github.anandhirulkar.supportiq.shared.exception.AiServiceUnavailableException;
import io.github.anandhirulkar.supportiq.shared.exception.BusinessRuleException;
import io.github.anandhirulkar.supportiq.shared.exception.DuplicateResourceException;
import io.github.anandhirulkar.supportiq.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Translates exceptions into RFC 9457 {@code application/problem+json} responses.
 * One consistent error contract keeps clients simple and avoids leaking stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "https://supportiq.dev/errors/";

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), request, "not-found");
    }

    @ExceptionHandler(DuplicateResourceException.class)
    ProblemDetail handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Resource already exists", ex.getMessage(), request, "duplicate");
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violated", ex.getMessage(), request, "business-rule");
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    ProblemDetail handleAiUnavailable(AiServiceUnavailableException ex, HttpServletRequest request) {
        log.warn("AI provider unavailable: {}", ex.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "AI provider unavailable", ex.getMessage(), request, "ai-unavailable");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid", request, "validation");
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST, "Validation failed", "One or more parameters are invalid", request, "validation");
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail handleUnreadableRequest(Exception ex, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "The request body or a parameter could not be parsed",
                request,
                "malformed-request");
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        detail.setTitle(ex.getStatusCode() == HttpStatus.FORBIDDEN ? "Forbidden" : "Request failed");
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                "The request could not be completed. Quote the instance for support.",
                request,
                "internal");
    }

    private ProblemDetail problem(
            HttpStatus status, String title, String detail, HttpServletRequest request, String slug) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(TYPE_PREFIX + slug));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
