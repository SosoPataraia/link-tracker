package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "Invalid request body",
                        "400",
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        Arrays.stream(ex.getStackTrace())
                                .map(StackTraceElement::toString)
                                .limit(5)
                                .toList()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleError(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ApiErrorResponse(
                        "Internal server error",
                        "500",
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        Arrays.stream(ex.getStackTrace())
                                .map(StackTraceElement::toString)
                                .limit(5)
                                .toList()));
    }
}
