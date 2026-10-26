package buss.smartbussingapi;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    // Limites de solicitudes por minuto segun el tipo de endpoint
    public static final int DEFAULT_LIMIT_PER_MINUTE = 100;
    public static final int LOGIN_LIMIT_PER_MINUTE = 5;
    public static final int VIAJE_LIMIT_PER_MINUTE = 30;

    private static final long WINDOW_MILLIS = 60_000L; // 1 minuto

    private static class RateWindow {
        private final ArrayDeque<Long> timestamps = new ArrayDeque<>();

        public synchronized boolean tryAcquire(long now, long windowStart, int maxAllowed) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxAllowed) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }

    // Mapa concurrente por clave (IP + grupo de ruta) -> ventana de peticiones
    private final Map<String, RateWindow> requestLog = new ConcurrentHashMap<>();

    /**
     * Evalua si una peticion debe permitirse o bloquearse segun el rate limit.
     */
    public boolean isAllowed(String clientIp, String uri) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "UNKNOWN";
        }

        String rateKey = resolveRateKey(clientIp, uri);
        int maxAllowed = resolveLimit(uri);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_MILLIS;

        RateWindow window = requestLog.computeIfAbsent(rateKey, k -> new RateWindow());
        return window.tryAcquire(now, windowStart, maxAllowed);
    }

    /**
     * Retorna los segundos estimados antes de que expire la ventana.
     */
    public long getRetryAfterSeconds() {
        return 60L;
    }

    public void resetAll() {
        requestLog.clear();
    }

    private String resolveRateKey(String clientIp, String uri) {
        if (uri != null && uri.contains("/user/login")) {
            return clientIp + ":LOGIN";
        } else if (uri != null && uri.contains("/viaje")) {
            return clientIp + ":VIAJE";
        }
        return clientIp + ":DEFAULT";
    }

    private int resolveLimit(String uri) {
        if (uri != null && uri.contains("/user/login")) {
            return LOGIN_LIMIT_PER_MINUTE;
        } else if (uri != null && uri.contains("/viaje")) {
            return VIAJE_LIMIT_PER_MINUTE;
        }
        return DEFAULT_LIMIT_PER_MINUTE;
    }
}
