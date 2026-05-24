package com.ga.leave.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory rate limiting filter for authentication endpoints.
 * For production, consider using Redis-based rate limiting or bucket4j.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long TIME_WINDOW_MS = TimeUnit.MINUTES.toMillis(1);

    // Map of IP address to request tracking
    private final Map<String, RequestTracker> requestCounts = new ConcurrentHashMap<>();

    // Endpoints to rate limit
    private static final String[] RATE_LIMITED_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate limit specific endpoints
        if (shouldRateLimit(path)) {
            String clientIp = getClientIP(request);

            if (isRateLimited(clientIp)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldRateLimit(String path) {
        for (String rateLimitedPath : RATE_LIMITED_PATHS) {
            if (path.equals(rateLimitedPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRateLimited(String clientIp) {
        long currentTime = System.currentTimeMillis();

        requestCounts.compute(clientIp, (key, tracker) -> {
            if (tracker == null || currentTime - tracker.windowStart > TIME_WINDOW_MS) {
                // New window
                return new RequestTracker(currentTime, 1);
            } else {
                // Same window, increment count
                tracker.count++;
                return tracker;
            }
        });

        RequestTracker tracker = requestCounts.get(clientIp);
        return tracker != null && tracker.count > MAX_REQUESTS_PER_MINUTE;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP if multiple are present
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Cleanup expired entries every 5 minutes to prevent memory leak
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().windowStart > TIME_WINDOW_MS * 2
        );
    }

    private static class RequestTracker {
        long windowStart;
        int count;

        RequestTracker(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

