package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(ResponseStatusException ex) {
        log.atWarn()
                .addKeyValue("status", ex.getStatusCode())
                .addKeyValue("reason", ex.getReason())
                .log("request.status.error");
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiErrorResponse(
                        ex.getReason(),
                        String.valueOf(ex.getStatusCode().value()),
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.atWarn().addKeyValue("exception", ex.getClass().getSimpleName()).log("request.validation.failed");
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "Invalid request",
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
        log.atError().addKeyValue("exception", ex.getClass().getSimpleName()).log("request.unhandled.error", ex);
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
