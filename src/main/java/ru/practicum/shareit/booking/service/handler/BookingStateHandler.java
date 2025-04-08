package ru.practicum.shareit.booking.service.handler;

import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;

public abstract class BookingStateHandler {
    private BookingStateHandler nextHandler;
    protected BookingRepository bookingRepository;

    public BookingStateHandler(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public BookingStateHandler setNext(BookingStateHandler handler) {
        this.nextHandler = handler;
        return handler;
    }

    public List<Booking> handle(BookingState state, Long userId, LocalDateTime now) {
        if (canHandle(state)) {
            return getBookings(userId, now);
        } else if (nextHandler != null) {
            return nextHandler.handle(state, userId, now);
        } else {
            throw new IllegalArgumentException("Неизвестный статус: " + state);
        }
    }

    protected abstract boolean canHandle(BookingState state);

    protected abstract List<Booking> getBookings(Long userId, LocalDateTime now);
}