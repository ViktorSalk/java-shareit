package ru.practicum.shareit.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ShareItException extends RuntimeException {
    private final HttpStatus status;

    public ShareItException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class NotFoundException extends ShareItException {
        public NotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class ConflictException extends ShareItException {
        public ConflictException(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }
}