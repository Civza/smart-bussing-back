package buss.smartbussingapi.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * KAN-17: registration payload. There is intentionally no {@code rol} field, so a client cannot
 * register itself as ADMIN. Password max is 72 because BCrypt ignores anything longer.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 255) String profilePhotoURL) {
}
