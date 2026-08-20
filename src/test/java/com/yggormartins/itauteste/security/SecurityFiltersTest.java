package com.yggormartins.itauteste.security;

import com.yggormartins.itauteste.config.AppProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityFiltersTest {
    @AfterEach
    void cleanupContext() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void correlationFilterAcceptsOnlySafeIds() throws Exception {
        var filter = new CorrelationIdFilter();
        var safeRequest = new MockHttpServletRequest("GET", "/estatistica");
        safeRequest.addHeader(CorrelationIdFilter.HEADER, "client-123");
        var safeResponse = new MockHttpServletResponse();
        filter.doFilter(safeRequest, safeResponse, new MockFilterChain());
        assertThat(safeResponse.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("client-123");

        var maliciousRequest = new MockHttpServletRequest("GET", "/estatistica");
        maliciousRequest.addHeader(CorrelationIdFilter.HEADER, "bad\r\nInjected: true");
        var maliciousResponse = new MockHttpServletResponse();
        filter.doFilter(maliciousRequest, maliciousResponse, new MockFilterChain());
        assertThat(maliciousResponse.getHeader(CorrelationIdFilter.HEADER))
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void payloadFilterRejectsDeclaredAndChunkedOversizedBodies() throws Exception {
        var filter = new PayloadSizeLimitFilter(properties(32, 10));
        var declared = new MockHttpServletRequest("POST", "/transacao");
        declared.setContent(new byte[33]);
        var declaredResponse = new MockHttpServletResponse();
        filter.doFilter(declared, declaredResponse, new MockFilterChain());
        assertThat(declaredResponse.getStatus()).isEqualTo(413);
        assertThat(declaredResponse.getContentAsString()).contains("payload_too_large");

        var accepted = new MockHttpServletRequest("POST", "/transacao");
        accepted.setContent("{}".getBytes(StandardCharsets.UTF_8));
        var acceptedResponse = new MockHttpServletResponse();
        filter.doFilter(accepted, acceptedResponse, new MockFilterChain());
        assertThat(acceptedResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void identityResolverIgnoresUnverifiedKeysAndHashesAuthenticatedSubjects() {
        var resolver = new ClientIdentityResolver();
        var request = new MockHttpServletRequest("GET", "/estatistica");
        request.setRemoteAddr("192.0.2.20");
        request.addHeader("X-API-Key", "super-secret-key");
        String apiKeyIdentity = resolver.resolve(request);
        assertThat(apiKeyIdentity).isEqualTo("ip:192.0.2.20");

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("customer-42", null, "ROLE_USER"));
        String subjectIdentity = resolver.resolve(request);
        assertThat(subjectIdentity).startsWith("subject:").doesNotContain("customer-42");
    }

    @Test
    void rateLimiterReturns429AndTemporaryBlock() throws Exception {
        AppProperties properties = properties(10_240, 1);
        var filter = new RateLimitFilter(new ClientIdentityResolver(), properties,
                new SimpleMeterRegistry());

        var firstRequest = new MockHttpServletRequest("GET", "/estatistica");
        firstRequest.setRemoteAddr("192.0.2.10");
        var firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());
        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(firstResponse.getHeader("X-RateLimit-Remaining")).isEqualTo("0");

        var secondRequest = new MockHttpServletRequest("GET", "/estatistica");
        secondRequest.setRemoteAddr("192.0.2.10");
        var secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getHeader("Retry-After")).isNotBlank();
        assertThat(secondResponse.getContentAsString()).contains("rate_limit_exceeded");
    }

    private static AppProperties properties(int maxPayload, long capacity) {
        var rateLimit = new AppProperties.RateLimit(capacity, capacity,
                Duration.ofMinutes(1), Duration.ofSeconds(2), 1_000);
        return new AppProperties(Duration.ofSeconds(60),
                new AppProperties.Security(rateLimit, maxPayload, false));
    }
}
