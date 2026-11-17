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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/** KAN-18 (T4): credentials sent in the query string are rejected with 400 and never echoed. */
@ExtendWith(MockitoExtension.class)
class SecurityAuditFilterCredentialsTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SecurityAuditFilter filter;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        RateLimiterService rateLimiterService = new RateLimiterService();
        rateLimiterService.resetAll();
        filter = new SecurityAuditFilter(rateLimiterService);

        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getMethod()).thenReturn("POST");
    }

    @Test
    void rejectsLoginWithPasswordInQueryString() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/user/login");
        when(request.getQueryString()).thenReturn("email=a@test.com&password=SuperSecret1");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertFalse(responseWriter.toString().contains("SuperSecret1"));
    }

    @Test
    void rejectsPasswordParameterOnAnyEndpointCaseInsensitive() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/user/me/password");
        when(request.getQueryString()).thenReturn("Password=abc12345");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void allowsQueryStringsWithoutCredentials() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/reporteRuta/byRuta");
        when(request.getQueryString()).thenReturn("routeName=Ruta1");
        when(response.getStatus()).thenReturn(200);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void parameterNamesThatOnlyContainTheWordAreNotBlocked() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/ruta");
        when(request.getQueryString()).thenReturn("forgotPasswordHint=1");
        when(response.getStatus()).thenReturn(200);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertTrue(responseWriter.toString().isEmpty());
    }
}
