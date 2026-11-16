package buss.smartbussingapi.commons;

import buss.smartbussingapi.commons.exceptions.AccessDeniedException;
import buss.smartbussingapi.commons.exceptions.AlreadyExistsException;
import buss.smartbussingapi.commons.exceptions.DlpViolationException;
import buss.smartbussingapi.commons.exceptions.InvalidCredentialsException;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import buss.smartbussingapi.commons.exceptions.RateLimitExceededException;
import buss.smartbussingapi.commons.exceptions.ResourceBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(NotFoundException e) {
        return new ApiResponse<>("Not found", null, e.getMessage());
    }

    @ExceptionHandler(InvalidDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidData(InvalidDataException e) {
        return new ApiResponse<>("Invalid data", null, e.getMessage());
    }

    /** KAN-17: Bean Validation failures on request DTOs. Only field names are echoed, never values. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String fields = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .reduce((a, b) -> a + "; " + b)
                .orElse("Solicitud invalida");
        return new ApiResponse<>("Invalid data", null, fields);
    }

    @ExceptionHandler(DlpViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDlpViolation(DlpViolationException e) {
        return new ApiResponse<>("DLP Violation", null, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMalformedJson(HttpMessageNotReadableException e) {
        log.warn("Peticion con JSON malformado recibida: {}", e.getMessage());
        return new ApiResponse<>("Bad Request", null, "El cuerpo de la solicitud (JSON) es invalido o esta mal formado.");
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleAlreadyExists(AlreadyExistsException e) {
        return new ApiResponse<>("Already exists", null, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleInvalidCredentials(InvalidCredentialsException e) {
        return new ApiResponse<>("Unauthorized", null, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException e) {
        return new ApiResponse<>("Forbidden", null, e.getMessage());
    }

    @ExceptionHandler(ResourceBlockedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleResourceBlocked(ResourceBlockedException e) {
        return new ApiResponse<>("Forbidden", null, e.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleRateLimitExceeded(RateLimitExceededException e) {
        return new ApiResponse<>("Too Many Requests", null, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGenericException(Exception e) {
        log.error("Error interno del servidor no controlado: ", e);
        // Sanitizado para evitar Information Exposure / fuga de stacktrace al cliente (CWE-209)
        return new ApiResponse<>("Internal Server Error", null, "Ha ocurrido un error interno en el servidor. Por favor contacte al soporte tecnico.");
    }
}
