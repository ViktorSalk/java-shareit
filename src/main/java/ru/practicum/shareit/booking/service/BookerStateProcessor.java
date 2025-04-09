package ru.practicum.shareit.booking.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.service.handler.booker.BookingStateHandler;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookerStateProcessor {
    private final List<BookingStateHandler> handlers;

    public BookerStateProcessor(@Qualifier("booker") List<BookingStateHandler> handlers) {
        this.handlers = handlers;
    }

    public List<Booking> process(BookingState state, Long userId, LocalDateTime now) {
        return handlers.stream()
                .filter(handler -> handler.canHandle(state))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Неизвестный статус: " + state))
                .getBookings(userId, now);
    }
}