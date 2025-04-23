package ru.practicum.shareit.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(final MethodArgumentNotValidException exception,
                                                   HttpServletRequest request) {
        log.error("Ошибка валидации: {}", exception.getMessage());
        return ErrorResponse.builder()
                .error("Ошибка валидации")
                .message(exception.getBindingResult().getFieldError().getDefaultMessage())
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(final IllegalArgumentException exception,
                                                        HttpServletRequest request) {
        log.error("Ошибка в аргументах запроса: {}", exception.getMessage());
        return ErrorResponse.builder()
                .error("Ошибка в аргументах запроса")
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(final Exception exception,
                                         HttpServletRequest request) {
        log.error("Непредвиденная ошибка: {}", exception.getMessage(), exception);
        return ErrorResponse.builder()
                .error("Непредвиденная ошибка")
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .path(request.getRequestURI())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
    }
}