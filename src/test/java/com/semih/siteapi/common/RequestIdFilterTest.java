package com.semih.siteapi.common;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void everyRequestGetsAnIdInTheLogsAndTheResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInChain = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), response,
                (req, res) -> seenInChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(seenInChain.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seenInChain.get());
        // Cleaned up, so the next request on this thread does not inherit it
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void aSafeIdFromAProxyIsKept() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc123-def456");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc123-def456");
    }

    @Test
    void anUnsafeIdIsReplaced() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // A line break here could fake extra log lines
        request.addHeader(RequestIdFilter.HEADER, "abc\nERROR fake log line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER))
                .doesNotContain("\n")
                .matches("[0-9a-f-]{36}");
    }
}
