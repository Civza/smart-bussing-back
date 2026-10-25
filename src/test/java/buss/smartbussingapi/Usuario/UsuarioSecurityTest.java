package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.commons.exceptions.InvalidCredentialsException;
import buss.smartbussingapi.commons.exceptions.ResourceBlockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UsuarioSecurityTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario user;

    @BeforeEach
    void setUp() {
        user = new Usuario();
        user.setIdUsuario(1);
        user.setEmail("security@test.com");
        user.setPassword("correct_password");
        usuarioService.resetLockout("security@test.com");
    }

    @Test
    void verifyCredential_LocksAccountAfterFiveFailedAttempts() {
        when(usuarioRepository.findByEmail("security@test.com")).thenReturn(Optional.of(user));

        // First 4 failed attempts should throw InvalidCredentialsException (HTTP 401)
        for (int i = 0; i < 4; i++) {
            assertThrows(InvalidCredentialsException.class, () ->
                    usuarioService.verifyCredential("security@test.com", "wrong_pass")
            );
        }

        // 5th failed attempt should trigger account lockout and throw ResourceBlockedException (HTTP 403)
        assertThrows(ResourceBlockedException.class, () ->
                usuarioService.verifyCredential("security@test.com", "wrong_pass")
        );

        // Subsequent attempt (even with correct password) should still be blocked (HTTP 403)
        assertThrows(ResourceBlockedException.class, () ->
                usuarioService.verifyCredential("security@test.com", "correct_password")
        );
    }

    @Test
    void verifyCredential_ResetsFailedAttemptsOnSuccessfulLogin() {
        when(usuarioRepository.findByEmail("security@test.com")).thenReturn(Optional.of(user));

        // 3 failed attempts
        for (int i = 0; i < 3; i++) {
            assertThrows(InvalidCredentialsException.class, () ->
                    usuarioService.verifyCredential("security@test.com", "wrong_pass")
            );
        }

        // 1 successful login resets counter
        boolean success = usuarioService.verifyCredential("security@test.com", "correct_password");
        assertTrue(success);

        // Next 4 failed attempts should again just be InvalidCredentialsException (not locked)
        for (int i = 0; i < 4; i++) {
            assertThrows(InvalidCredentialsException.class, () ->
                    usuarioService.verifyCredential("security@test.com", "wrong_pass")
            );
        }
    }
}
