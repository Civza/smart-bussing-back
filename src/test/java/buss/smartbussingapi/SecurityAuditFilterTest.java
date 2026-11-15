package buss.smartbussingapi;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityAuditFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimiterService rateLimiterService;
    private SecurityAuditFilter filter;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        rateLimiterService = new RateLimiterService();
        rateLimiterService.resetAll();
        filter = new SecurityAuditFilter(rateLimiterService);

        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getMethod()).thenReturn("GET");
    }

    @Test
    void allowsSafeRequestAndAddsSecurityHeaders() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/ruta");
        when(request.getQueryString()).thenReturn(null);
        when(response.getStatus()).thenReturn(200);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "DENY");
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    }

    @Test
    void blocksSqlInjectionPattern() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/user");
        when(request.getQueryString()).thenReturn("email=' OR '1'='1");

        filter.doFilter(request, response, filterChain);

        // Chain should NOT be called
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(responseWriter.toString().contains("SQL_INJECTION"));
    }

    @Test
    void blocksPathTraversalPattern() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/static/../../secret.txt");
        when(request.getQueryString()).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(responseWriter.toString().contains("PATH_TRAVERSAL"));
    }

    @Test
    void blocksXssPattern() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/user/search");
        when(request.getQueryString()).thenReturn("name=<script>alert(1)</script>");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(responseWriter.toString().contains("XSS_ATTACK"));
    }

    @Test
    void blocksWhenRateLimitExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/user/login");
        when(request.getQueryString()).thenReturn(null);

        // Exhaust login limit (5 requests)
        for (int i = 0; i < RateLimiterService.LOGIN_LIMIT_PER_MINUTE; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // 6th request should be blocked by rate limit with HTTP 429
        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
        assertTrue(responseWriter.toString().contains("limite permitido"));
    }
}
