package buss.smartbussingapi.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public "leave your email" form on the landing page. */
public record InteresadoRequest(@NotBlank @Email @Size(max = 255) String email) {
}
