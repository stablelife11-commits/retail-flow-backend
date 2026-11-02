package com.retail.flow.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_IPS = 10_000;

    @Value("${app.security.auth-rate-limit-per-minute:20}")
    private int maxRequestsPerMinute;

    private final Map<String, RateWindow> ipWindows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Relies on server.forward-headers-strategy=native so only trusted proxies can set remoteAddr
        String clientIp = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        long now = System.currentTimeMillis();

        // Evict only expired windows so active brute-force counters cannot be flushed by attackers
        if (ipWindows.size() > MAX_TRACKED_IPS) {
            ipWindows.entrySet().removeIf(entry -> (now - entry.getValue().windowStartMs) > WINDOW_MS);
        }

        RateWindow window = ipWindows.compute(clientIp, (ip, existing) -> {
            if (existing == null || (now - existing.windowStartMs) > WINDOW_MS) {
                return new RateWindow(now, new AtomicInteger(1));
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        if (window.counter.get() > maxRequestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many authentication attempts. Please wait a minute and try again.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private record RateWindow(long windowStartMs, AtomicInteger counter) {}
}