package com.vdt2026.omnicare.channel.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTests {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void echoesProvidedCorrelationIdAndClearsMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-http-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new AssertingFilterChain("corr-http-1"));

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("corr-http-1");
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void createsCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isNotBlank();
    }

    private static class AssertingFilterChain extends MockFilterChain {

        private final String expectedCorrelationId;

        AssertingFilterChain(String expectedCorrelationId) {
            this.expectedCorrelationId = expectedCorrelationId;
        }

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
            throws IOException, ServletException {
            assertThat(MDC.get("correlationId")).isEqualTo(expectedCorrelationId);
            assertThat(MDC.get("traceId")).isEqualTo(expectedCorrelationId);
            super.doFilter(request, response);
        }
    }
}
