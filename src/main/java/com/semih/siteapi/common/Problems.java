package com.semih.siteapi.common;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.time.Instant;

/**
 * Builds the API's single error shape: an RFC 9457 problem detail plus a {@code timestamp}.
 * Used by both {@link GlobalExceptionHandler} (controller errors) and the security handler
 * (401/403 from the filter chain), so the two can never drift apart.
 */
public final class Problems {

    public static final String TIMESTAMP = "timestamp";

    private Problems() {
    }

    public static ProblemDetail of(HttpStatusCode status, String title, String detail, Instant now) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty(TIMESTAMP, now);
        return problem;
    }

    /** Adds the timestamp to a problem Spring built itself (bad JSON, 405, 415...), if it is missing. */
    public static void stamp(ProblemDetail problem, Instant now) {
        if (problem.getProperties() == null || !problem.getProperties().containsKey(TIMESTAMP)) {
            problem.setProperty(TIMESTAMP, now);
        }
    }
}
