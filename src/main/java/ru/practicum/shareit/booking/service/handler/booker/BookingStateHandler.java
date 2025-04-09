package ru.practicum.shareit.booking.service.handler.booker;

import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.model.Booking;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingStateHandler {
    boolean canHandle(BookingState state);

    List<Booking> getBookings(Long userId, LocalDateTime now);
}