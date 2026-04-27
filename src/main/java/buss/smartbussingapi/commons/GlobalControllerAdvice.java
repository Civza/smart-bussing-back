package buss.smartbussingapi.commons;

import buss.smartbussingapi.commons.exceptions.AlreadyExistsException;
import buss.smartbussingapi.commons.exceptions.InvalidCredentialsException;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(NotFoundException e) {
        return new ApiResponse<>("Not found", null, e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidData(InvalidDataException e) {
        return new ApiResponse<>("Invalid data", null, e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleAlreadyExists(AlreadyExistsException e) {
        return new ApiResponse<>("Already exists", null, e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleInvalidCredentials(InvalidCredentialsException e) {
        return new ApiResponse<>("Unauthorized", null, e.getMessage());
    }
}
