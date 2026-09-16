package com.internalmarketplace.api.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internalmarketplace.api.common.exception.ApiError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate-limits write-heavy/sensitive endpoints, matching app.js's three
 * matchers exactly: any non-GET under /posts, all of /reports, and any POST
 * ending in /comments. In-memory and IP-keyed, same semantics (and the same
 * per-instance-only limitation) as the Node app's express-rate-limit
 * middleware — Cloud Run autoscaling to N instances multiplies the effective
 * limit today too, this is not a regression. A Redis-backed Bucket4j proxy
 * manager is the upgrade path if a cluster-wide limit is later required.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final Duration refillPeriod;
    private final ObjectMapper objectMapper;

    /**
     * Constructed explicitly by SecurityConfig (not component-scanned) so it is
     * wired into the Spring Security filter chain exactly once, instead of
     * also being auto-registered as a top-level servlet filter by Spring Boot.
     */
    public RateLimitFilter(int capacity, long refillPeriodSeconds, ObjectMapper objectMapper) {
        this.capacity = capacity;
        this.refillPeriod = Duration.ofSeconds(refillPeriodSeconds);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isRateLimitedRoute(request);
    }

    private boolean isRateLimitedRoute(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean postsWrite = path.startsWith("/api/v1/posts") && !"GET".equalsIgnoreCase(method);
        boolean reports = path.startsWith("/api/v1/reports");
        boolean commentsCreate = path.endsWith("/comments") && "POST".equalsIgnoreCase(method);
        return postsWrite || reports || commentsCreate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Bucket bucket = buckets.computeIfAbsent(clientKey(request), key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("RateLimit-Limit", String.valueOf(capacity));

        if (probe.isConsumed()) {
            response.setHeader("RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        response.setHeader("RateLimit-Remaining", "0");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setStatus(429); // HttpServletResponse has no SC_TOO_MANY_REQUESTS constant
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiError.of("Too many requests, please try again later.")));
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillPeriod));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
