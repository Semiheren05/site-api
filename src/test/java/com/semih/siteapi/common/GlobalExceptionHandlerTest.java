package com.semih.siteapi.common;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void aConstraintViolationIsLoggedByNameWithoutTheRejectedValue(CapturedOutput output) {
        SQLException sql = new SQLException(
                "duplicate key value violates unique constraint \"uk_users_email\" "
                        + "Detail: Key (email)=(semih@example.com) already exists.");
        var ex = new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("could not execute statement", sql, "uk_users_email"));

        ProblemDetail problem = handler.handleDataIntegrity(ex);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties()).containsEntry("timestamp", NOW);
        assertThat(output).contains("constraint=uk_users_email").doesNotContain("semih@example.com");
    }
}
