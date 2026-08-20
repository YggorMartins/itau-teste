package com.yggormartins.itauteste.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yggormartins.itauteste.config.AppProperties;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Rate limit local. Para múltiplas réplicas, mover a política ao gateway/Redis. */
public final class RateLimitFilter extends OncePerRequestFilter {
    private final ClientIdentityResolver identityResolver;
    private final Cache<String, ClientLimit> clients;
    private final AppProperties.RateLimit properties;
    private final Counter allowedCounter;
    private final Counter rejectedCounter;

    public RateLimitFilter(ClientIdentityResolver identityResolver,
                           AppProperties appProperties,
                           MeterRegistry meterRegistry) {
        this.identityResolver = identityResolver;
        this.properties = appProperties.security().rateLimit();
        Duration expiry = properties.refillPeriod().plus(properties.temporaryBlock()).multipliedBy(2);
        this.clients = Caffeine.newBuilder()
                .maximumSize(properties.maximumTrackedClients())
                .expireAfterAccess(expiry)
                .build();
        this.allowedCounter = meterRegistry.counter("http.rate_limit.requests", "result", "allowed");
        this.rejectedCounter = meterRegistry.counter("http.rate_limit.requests", "result", "rejected");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || path.startsWith("/actuator/health/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = identityResolver.resolve(request);
        ClientLimit limit = clients.get(clientKey, ignored -> newClientLimit());
        long now = System.nanoTime();
        long blockedNanos = limit.blockedUntilNanos.get() - now;

        if (blockedNanos > 0) {
            reject(request, response, blockedNanos);
            return;
        }

        ConsumptionProbe probe = limit.bucket.tryConsumeAndReturnRemaining(1);
        response.setHeader("X-RateLimit-Limit", Long.toString(properties.capacity()));
        response.setHeader("X-RateLimit-Remaining", Long.toString(probe.getRemainingTokens()));
        if (!probe.isConsumed()) {
            long temporaryBlockNanos = properties.temporaryBlock().toNanos();
            limit.blockedUntilNanos.set(now + temporaryBlockNanos);
            reject(request, response, Math.max(temporaryBlockNanos,
                    probe.getNanosToWaitForRefill()));
            return;
        }

        allowedCounter.increment();
        filterChain.doFilter(request, response);
    }

    private ClientLimit newClientLimit() {
        Bucket bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(properties.capacity())
                        .refillGreedy(properties.refillTokens(), properties.refillPeriod()))
                .build();
        return new ClientLimit(bucket);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response,
                        long waitNanos) throws IOException {
        rejectedCounter.increment();
        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(waitNanos) + 1);
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        SecurityProblemWriter.write(request, response,
                HttpStatus.TOO_MANY_REQUESTS.value(), "Muitas requisições",
                "rate_limit_exceeded", "Limite temporário de requisições excedido");
    }

    private static final class ClientLimit {
        private final Bucket bucket;
        private final AtomicLong blockedUntilNanos = new AtomicLong();

        private ClientLimit(Bucket bucket) {
            this.bucket = bucket;
        }
    }
}
