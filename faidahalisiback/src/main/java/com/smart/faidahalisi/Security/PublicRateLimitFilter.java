package com.smart.faidahalisi.Security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Kinga rahisi dhidi ya spam kwenye njia za umma: kuweka oda, ufuatiliaji, kuingia (login) na kuchagua malipo.
 * Inahesabu maombi kwa IP kwa kila saa. Hii ni kinga ya msingi ndani ya kumbukumbu ya app moja;
 * kwa production kubwa (seva nyingi / nyuma ya proxy) tumia rate limiting ya proxy au gateway.
 */
@Component
public class PublicRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 60L * 60L * 1000L;

    private static final class Counter {
        long windowStart;
        int count;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final int maxOrders;
    private final int maxTracking;

    public PublicRateLimitFilter(
            @Value("${app.public.max-orders-per-hour:20}") int maxOrders,
            @Value("${app.public.max-tracking-per-hour:240}") int maxTracking) {
        this.maxOrders = maxOrders;
        this.maxTracking = maxTracking;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String bucket = null;
        int limit = 0;
        if ("POST".equals(method) && (path.equals("/api/orders/guest") || path.startsWith("/api/payments/order-number/"))) {
            bucket = "order";
            limit = maxOrders;
        } else if ("GET".equals(method) && path.startsWith("/api/orders/track/")) {
            bucket = "track";
            limit = maxTracking;
        } else if ("POST".equals(method) && path.equals("/api/auth/login")) {
            bucket = "login";
            limit = Math.max(maxOrders * 3, 30);
        }
        if (bucket != null && !allow(bucket + ":" + request.getRemoteAddr(), limit)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Maombi ni mengi mno. Jaribu tena baadaye.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean allow(String key, int limit) {
        long now = System.currentTimeMillis();
        if (counters.size() > 50_000) {
            counters.clear(); // kinga ya kumbukumbu; inafuta kila kitu kwa urahisi
        }
        Counter c = counters.computeIfAbsent(key, k -> new Counter());
        synchronized (c) {
            if (now - c.windowStart > WINDOW_MS) {
                c.windowStart = now;
                c.count = 0;
            }
            c.count++;
            return c.count <= limit;
        }
    }
}
