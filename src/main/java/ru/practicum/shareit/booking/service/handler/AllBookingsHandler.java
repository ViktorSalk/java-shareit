package ru.practicum.shareit.booking.service.handler;

import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;

public class AllBookingsHandler extends BookingStateHandler {

    public AllBookingsHandler(BookingRepository bookingRepository) {
        super(bookingRepository);
    }

    @Override
    protected boolean canHandle(BookingState state) {
        return state == BookingState.ALL;
    }

    @Override
    protected List<Booking> getBookings(Long userId, LocalDateTime now) {
        return bookingRepository.findAllByBookerId(userId);
    }
}