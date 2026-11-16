package buss.smartbussingapi.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** KAN-18: changing the password requires the current one. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
