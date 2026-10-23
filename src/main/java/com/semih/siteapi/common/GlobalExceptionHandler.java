package com.semih.siteapi.common;

import com.semih.siteapi.common.exception.EmailAlreadyUsedException;
import com.semih.siteapi.common.exception.InvalidCredentialsException;
import com.semih.siteapi.common.exception.InvalidDateRangeException;
import com.semih.siteapi.common.exception.InvalidRefreshTokenException;
import com.semih.siteapi.common.exception.InvalidSlugException;
import com.semih.siteapi.common.exception.InvalidTagException;
import com.semih.siteapi.common.exception.RateLimitExceededException;
import com.semih.siteapi.common.exception.ResourceNotFoundException;
import com.semih.siteapi.common.exception.SlugAlreadyUsedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into RFC 9457 "problem detail" JSON responses, so every error the API
 * returns has the same shape: type, title, status, detail, instance and timestamp.
 *
 * <p>Logging follows one rule: an error is logged once, here, and only when someone should
 * look at it. Client mistakes (400, 404, 409) are normal traffic and are not logged.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /** 400 with a field -> message map, e.g. {"errors": {"email": "must be a well-formed email address"}}. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation error", "Request validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Spring's own errors (unreadable JSON, 405, 415...) pass through here, so they get a timestamp too. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            Problems.stamp(problem, clock.instant());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        return problem(HttpStatus.CONFLICT, "Email already in use", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid refresh token", ex.getMessage());
    }

    @ExceptionHandler(SlugAlreadyUsedException.class)
    ProblemDetail handleSlugAlreadyUsed(SlugAlreadyUsedException ex) {
        return problem(HttpStatus.CONFLICT, "Slug already in use", ex.getMessage());
    }

    @ExceptionHandler(InvalidTagException.class)
    ProblemDetail handleInvalidTag(InvalidTagException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid tag", ex.getMessage());
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    ProblemDetail handleInvalidDateRange(InvalidDateRangeException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid date range", ex.getMessage());
    }

    @ExceptionHandler(InvalidSlugException.class)
    ProblemDetail handleInvalidSlug(InvalidSlugException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid slug", ex.getMessage());
    }

    /** 429 with a Retry-After header (seconds), so clients know when to try again. */
    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(problem(HttpStatus.TOO_MANY_REQUESTS, "Too many requests", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    /** E.g. two concurrent registrations with the same email: the unique constraint wins. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict", "The request conflicts with existing data");
    }

    /** Last resort: a bug. Log everything, but never leak stack traces or internals to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error method={} path={}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "Something went wrong");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        return Problems.of(status, title, detail, clock.instant());
    }
}
