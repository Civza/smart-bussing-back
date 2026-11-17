package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.commons.exceptions.InvalidCredentialsException;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** KAN-16 (T2) password hashing and KAN-17 (T3) role mass-assignment, service layer. */
@ExtendWith(MockitoExtension.class)
class UsuarioServicePasswordTest {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario user;

    @BeforeEach
    void setUp() {
        user = new Usuario();
        user.setIdUsuario(7);
        user.setNombre("Ana");
        user.setEmail("ana@test.com");
        user.setPassword("password123");
        usuarioService.resetLockout("ana@test.com");
    }

    @Test
    void registerStoresABcryptHashNeverThePlainPassword() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        usuarioService.registerUsuario(user);

        ArgumentCaptor<Usuario> saved = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(saved.capture());
        String stored = saved.getValue().getPassword();
        assertTrue(stored.startsWith("$2"), "password must be stored as BCrypt, was: " + stored);
        assertTrue(BCRYPT.matches("password123", stored));
    }

    @Test
    void registerAlwaysAssignsUserRoleEvenIfAdminWasRequested() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        user.setRol(Rol.ADMIN);

        usuarioService.registerUsuario(user);

        ArgumentCaptor<Usuario> saved = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(saved.capture());
        assertEquals(Rol.USER, saved.getValue().getRol());
    }

    @Test
    void verifyCredentialAcceptsBcryptHashedPassword() {
        user.setPassword(BCRYPT.encode("password123"));
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        assertTrue(usuarioService.verifyCredential("ana@test.com", "password123"));
    }

    @Test
    void verifyCredentialRejectsWrongPasswordAgainstBcryptHash() {
        user.setPassword(BCRYPT.encode("password123"));
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> usuarioService.verifyCredential("ana@test.com", "wrong-password"));
    }

    @Test
    void legacyPlaintextPasswordIsRehashedOnSuccessfulLogin() {
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        assertTrue(usuarioService.verifyCredential("ana@test.com", "password123"));

        ArgumentCaptor<Usuario> saved = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(saved.capture());
        assertTrue(saved.getValue().getPassword().startsWith("$2"));
        assertTrue(BCRYPT.matches("password123", saved.getValue().getPassword()));
    }

    @Test
    void googleAccountsCannotLogInWithTheOauthPlaceholderPassword() {
        user.setPassword("OAUTH_GOOGLE");
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> usuarioService.verifyCredential("ana@test.com", "OAUTH_GOOGLE"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void accountsWithoutPasswordCannotLogInLocally() {
        user.setPassword(null);
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> usuarioService.verifyCredential("ana@test.com", "anything"));
    }

    @Test
    void changePasswordRequiresTheCurrentPassword() {
        user.setPassword(BCRYPT.encode("password123"));
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> usuarioService.changePassword(7, "not-my-password", "newPassword456"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void changePasswordStoresTheNewPasswordHashed() {
        user.setPassword(BCRYPT.encode("password123"));
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(user));

        usuarioService.changePassword(7, "password123", "newPassword456");

        verify(usuarioRepository).save(user);
        assertTrue(user.getPassword().startsWith("$2"));
        assertTrue(BCRYPT.matches("newPassword456", user.getPassword()));
    }

    @Test
    void changePasswordRejectsBlankNewPassword() {
        assertThrows(InvalidDataException.class, () -> usuarioService.changePassword(7, "password123", " "));
    }

    private static Usuario any() {
        return org.mockito.ArgumentMatchers.any(Usuario.class);
    }
}
