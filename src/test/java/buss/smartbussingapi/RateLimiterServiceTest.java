package buss.smartbussingapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        rateLimiterService.resetAll();
    }

    @Test
    void allowsRequestsWithinDefaultLimit() {
        String ip = "192.168.1.10";
        String uri = "/api/v1/ruta";

        for (int i = 0; i < RateLimiterService.DEFAULT_LIMIT_PER_MINUTE; i++) {
            assertTrue(rateLimiterService.isAllowed(ip, uri), "Request " + i + " should be allowed");
        }

        assertFalse(rateLimiterService.isAllowed(ip, uri), "Request exceeding limit should be blocked");
    }

    @Test
    void blocksLoginRequestsExceedingLimit() {
        String ip = "192.168.1.20";
        String uri = "/api/v1/user/login";

        for (int i = 0; i < RateLimiterService.LOGIN_LIMIT_PER_MINUTE; i++) {
            assertTrue(rateLimiterService.isAllowed(ip, uri), "Login request " + i + " should be allowed");
        }

        assertFalse(rateLimiterService.isAllowed(ip, uri), "Login request exceeding limit should be blocked");
    }

    @Test
    void blocksViajeRequestsExceedingLimit() {
        String ip = "192.168.1.30";
        String uri = "/api/v1/viaje/draft/get-travel";

        for (int i = 0; i < RateLimiterService.VIAJE_LIMIT_PER_MINUTE; i++) {
            assertTrue(rateLimiterService.isAllowed(ip, uri), "Viaje request " + i + " should be allowed");
        }

        assertFalse(rateLimiterService.isAllowed(ip, uri), "Viaje request exceeding limit should be blocked");
    }

    @Test
    void isolatesLimitsByIp() {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";
        String uri = "/api/v1/user/login";

        for (int i = 0; i < RateLimiterService.LOGIN_LIMIT_PER_MINUTE; i++) {
            assertTrue(rateLimiterService.isAllowed(ip1, uri));
        }
        assertFalse(rateLimiterService.isAllowed(ip1, uri));

        // ip2 should still be allowed
        assertTrue(rateLimiterService.isAllowed(ip2, uri));
    }

    @Test
    void handlesNullOrEmptyIpGracefully() {
        assertTrue(rateLimiterService.isAllowed(null, "/api/v1/ruta"));
        assertTrue(rateLimiterService.isAllowed("", "/api/v1/ruta"));
    }
}
