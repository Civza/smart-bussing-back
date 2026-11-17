package buss.smartbussingapi.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** KAN-18: credentials travel in the JSON body, never in the query string. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
